# Recipes API

A REST API for managing recipes, built with Spring Boot and deployed as an AWS Lambda function behind API Gateway.

## Features

- List all recipes
- Get a recipe by ID
- Create a new recipe
- OpenAPI / Swagger UI documentation
- Input validation with meaningful error responses

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

## OpenAPI / Code Generation

The API contract is defined in:

```
recipes-api/src/main/resources/openapi/openapi.yaml
```

Controller interfaces and request/response models are generated automatically during `mvn generate-sources`. Do not edit
generated files under `target/generated-sources/`.
