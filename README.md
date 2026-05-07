# Recipes API

A REST API for managing recipes, built with Spring Boot and deployed as an AWS Lambda function behind API Gateway.

## Features

- List all recipes
- Get a recipe by ID
- Create a new recipe
- OpenAPI / Swagger UI documentation
- Input validation with meaningful error responses

## Architecture decisions

**Spring Boot as the framework**
Picked Spring Boot because it's the framework I'm most familiar with and the
fastest to scaffold a working REST API in. The tradeoff is cold start time —
on Lambda, Spring Boot context init takes 5–8 seconds because of runtime
reflection-based dependency injection.

**Single Lambda for the whole API via `aws-serverless-java-container`**
The entire Spring Boot app runs inside one Lambda function, with
`aws-serverless-java-container` bridging API Gateway events to Spring MVC.
Simpler deployment surface — one function, one log group, one IAM role —
at the cost of all endpoints sharing the same cold start and scaling
together. The alternative would be one Lambda per endpoint for independent
scaling and smaller deploy units, at the cost of more configuration and
more cold-start surface area.

**Spec-first OpenAPI with code generation**
The OpenAPI spec is the source of truth — controller interfaces and
request/response models are generated from it during `mvn generate-sources`.
Gives consistent documentation, generated client code, and a single place
to review API changes.

**In-memory data store**
No database. Recipes live in a list in memory and are lost on cold start.
Deliberate choice — the goal of this project was getting Spring Boot to
deploy and run on Lambda end-to-end, not modelling a real persistence
layer.

**Multi-module Maven build (`repository` / `service` / `api`)**
Split into three modules to enforce separation of concerns at build time
— the `api` module depends on `service`, `service` depends on `repository`,
no upward dependencies.

**Maven shade over Spring Boot repackage**
Lambda's classloader requires classes at the root of the JAR; Spring Boot's
default nested-JAR layout (`BOOT-INF/classes/`) doesn't work. Shade plugin
produces a flat uber JAR with the right merging for SPI services and
auto-configuration descriptors. See **Deployment Challenges** below for the
full story.

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 17                             |
| Framework      | Spring Boot 3.4.3                   |
| Deployment     | AWS Lambda + API Gateway            |
| Lambda adapter | aws-serverless-java-container 2.0.0 |
| API spec       | OpenAPI 3.0 (code-generated)        |
| Build          | Maven (multi-module)                |
| Packaging      | maven-shade-plugin (flat uber JAR)  |

## Project Structure

```
recipes-api/               ← parent POM
├── recipes-repository/    ← domain models, in-memory data store
├── recipes-service/       ← business logic
└── recipes-api/           ← REST controllers, Lambda handler, OpenAPI spec
    └── src/main/resources/openapi/openapi.yaml  ← API contract
```

## API Endpoints

| Method | Path            | Description         |
|--------|-----------------|---------------------|
| `GET`  | `/recipes`      | List all recipes    |
| `GET`  | `/recipes/{id}` | Get a recipe by ID  |
| `POST` | `/recipes`      | Create a new recipe |

### Example request

```http
POST /recipes
Content-Type: application/json

{
  "name": "Spaghetti Carbonara",
  "cuisine": "Italian",
  "prepTimeMinutes": 30,
  "ingredients": ["spaghetti", "eggs", "pancetta", "parmesan"],
  "steps": ["Boil pasta", "Fry pancetta", "Mix eggs and cheese", "Combine"]
}
```

### Example response

```json
{
  "id": "abc-123",
  "name": "Spaghetti Carbonara",
  "cuisine": "Italian",
  "prepTimeMinutes": 30,
  "ingredients": [
    "spaghetti",
    "eggs",
    "pancetta",
    "parmesan"
  ],
  "steps": [
    "Boil pasta",
    "Fry pancetta",
    "Mix eggs and cheese",
    "Combine"
  ]
}
```

## Prerequisites

| Tool                  | Purpose               | Required for       |
|-----------------------|-----------------------|--------------------|
| Java 17               | Runtime               | All                |
| IntelliJ IDEA         | IDE                   | Local dev          |
| Docker Desktop        | Lambda emulation      | SAM local          |
| AWS SAM CLI           | Local Lambda + deploy | SAM local + deploy |
| AWS CLI + credentials | Deployment            | AWS deploy only    |

## Running Locally

### Option 1 — Tomcat (recommended for development)

Runs as a standard Spring Boot app. Full IntelliJ debugger support.

**Setup (one-time):**

1. Maven tool window → Profiles → enable `local` → reload Maven
2. Edit Run Configuration → Spring Boot tab → Active profiles → `local`

**Run:** `RecipesApplication.main()` in IntelliJ

**URLs:**

| URL                                           | Description  |
|-----------------------------------------------|--------------|
| `http://localhost:8080/recipes`               | API          |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI   |
| `http://localhost:8080/v3/api-docs`           | OpenAPI spec |

---

### Option 2 — SAM Local (Lambda emulation)

Runs inside a Docker container using the real AWS Lambda Java 17 runtime. Tests the actual Lambda execution path before
deploying.

**Prerequisites:** Docker running, SAM CLI installed.

**Build** (Maven `local` profile must be OFF):

```bash
mvn clean package -DskipTests
sam build
```

**Run:**

```bash
# Default — container is destroyed and recreated on every request (cold start each time, not realistic)
sam local start-api

# Recommended — container stays warm between requests, matching real Lambda behaviour
sam local start-api --warm-containers LAZY

# Eager warm — container starts immediately so even the first request is fast
sam local start-api --warm-containers EAGER
```

API available at `http://localhost:3000/recipes`.

> The first request always triggers a cold start (~60 seconds — Docker container startup + Spring Boot context init).
> Subsequent requests on a warm container respond in milliseconds.
> Use `--warm-containers LAZY` or `EAGER` to avoid a cold start on every request.

---

## Debugging

### Debug with Tomcat (IntelliJ)

Run `RecipesApplication` using the IntelliJ debug button. Breakpoints work in all classes.

### Debug with SAM Local (remote debugger)

1. Start SAM in debug mode:
   ```bash
   sam local start-api -d 5858 --warm-containers LAZY
   ```
2. Set a breakpoint (e.g. in `StreamLambdaHandler` or any controller)
3. Send a request to `http://localhost:3000/recipes`
4. In IntelliJ, click the debug button on the **SAM Debug** remote configuration (localhost:5858)
5. The JVM resumes and the breakpoint is hit

**IntelliJ remote debug configuration:**

- Type: Remote JVM Debug
- Host: `localhost`
- Port: `5858`

> Connect within 120 seconds of sending the request or the Lambda will time out.

---

## Deploying to AWS

**Prerequisites:** AWS CLI configured (`aws configure`) with Lambda, API Gateway, CloudFormation, S3, and IAM
permissions.

**Build** (Maven `local` profile must be OFF):

```bash
mvn clean package -DskipTests
sam build
```

**First-time deploy:**

```bash
sam deploy --guided
```

Follow the prompts:

| Prompt                            | Value                 |
|-----------------------------------|-----------------------|
| Stack name                        | `recipes-api`         |
| AWS region                        | e.g. `ap-southeast-2` |
| Confirm changes before deploy     | `Y`                   |
| Allow SAM to create IAM roles     | `Y`                   |
| RecipesFunction has no auth — OK? | `Y`                   |
| Save to samconfig.toml            | `Y`                   |

**Subsequent deploys:**

```bash
sam deploy
```

After deploy, SAM prints the live API Gateway URL:

```
Outputs:
  RecipesFunctionApi - https://<id>.execute-api.<region>.amazonaws.com/Prod/
```

---

## Deployment Challenges

A few non-obvious problems had to be solved to get `StreamLambdaHandler` deploying and running correctly on Lambda.

### 1. JAR layout — flat uber JAR vs Spring Boot nested JAR

AWS Lambda's classloader requires all `.class` files to be at the **root** of the JAR. Spring Boot's default repackage
plugin produces a **nested JAR** where classes live under `BOOT-INF/classes/`, which Lambda cannot load. This means
`StreamLambdaHandler` was simply not found at startup.

The fix is `maven-shade-plugin`, which merges all dependencies into a single flat uber JAR with classes at the root.
Spring Boot's repackage goal is explicitly disabled in `recipes-api/pom.xml` to prevent it from re-wrapping the shade
output.

The shade plugin is also configured with resource transformers to handle two merge conflicts that arise when flattening
many JARs into one:

- `ServicesResourceTransformer` — merges `META-INF/services/` SPI files; without this, only one JAR's service
  registrations win, breaking Jackson, Hibernate Validator, and other SPI-based providers.
- `AppendingTransformer` (x2) — merges Spring Boot auto-configuration descriptor files (`spring.factories` and
  `AutoConfiguration.imports`); without this, only one module's auto-configurations are registered and Spring context
  startup fails.

Signed JAR entries (`*.SF`, `*.DSA`, `*.RSA`) are also excluded to prevent `SecurityException` at runtime when the
merged JAR's signatures no longer match.

### 2. SAM build — getting the right JAR into the staging directory

SAM's default Maven build workflow doesn't know where the shade-produced JAR lands, and it re-runs Maven itself (which
triggers Checkstyle and other lifecycle phases). To take control of the staging step, `template.yaml` sets
`BuildMethod: makefile` on the function, delegating to `recipes-api/Makefile`.

The Makefile does one thing: copy the pre-built fat JAR from `target/` into `ARTIFACTS_DIR/lib/`, which is the path SAM
packages and uploads to S3. Without this, SAM was either uploading the wrong artifact or failing to find the handler
class entirely.

### 3. `samconfig.toml` — persisting deployment parameters

`sam deploy --guided` generates `samconfig.toml`, which stores the stack name, AWS region, S3 prefix, and IAM
capability settings. This allows all subsequent deploys to run as plain `sam deploy` without repeating the interactive
prompts. It is checked into source control so the deployment configuration is reproducible.

---

## OpenAPI / Code Generation

The API contract is defined in:

```
recipes-api/src/main/resources/openapi/openapi.yaml
```

Controller interfaces and request/response models are generated automatically during `mvn generate-sources`. Do not edit
generated files under `target/generated-sources/`.