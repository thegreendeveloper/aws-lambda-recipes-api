# Recipes API — Claude Context

A Spring Boot REST API designed to run as an AWS Lambda function, exposed via API Gateway. Manages recipe data (list,
get by ID, create).

---

## Architecture

Three-layer Maven multi-module project:

```
recipes-repository   ← in-memory data store, domain entities
       ↓
recipes-service      ← business logic (RecipesService)
       ↓
recipes-api-rest     ← REST controllers, Lambda handler, OpenAPI spec, fat JAR
```

| Module               | Artifact ID          | Description                                                           |
|----------------------|----------------------|-----------------------------------------------------------------------|
| `recipes-repository` | `recipes-repository` | Domain models, in-memory repository                                   |
| `recipes-service`    | `recipes-service`    | Business logic, depends on repository                                 |
| `recipes-api`        | `recipes-api-rest`   | Controllers, Lambda handler, OpenAPI codegen, produces deployable JAR |

**Key classes:**

- `RecipesApplication` — Spring Boot entry point (`@SpringBootApplication`)
- `StreamLambdaHandler` — AWS Lambda entry point; bridges Lambda events to Spring MVC via
  `SpringBootLambdaContainerHandler`
- `RecipesController` — implements generated `RecipesApi` interface; delegates to `RecipesService`
- `GlobalExceptionHandler` — `@RestControllerAdvice` for consistent error responses

---

## Technology Stack

- **Java 17**, **Spring Boot 3.4.3**
- **AWS Lambda** via `aws-serverless-java-container-springboot3` (v2.0.0)
- **maven-shade-plugin** — produces the Lambda deployment JAR (flat uber JAR with all classes at root, required for
  Lambda's classloader)
- **OpenAPI Generator** (v7.5.0) — API interfaces and models generated from
  `recipes-api/src/main/resources/openapi/openapi.yaml` at build time into `com.recipes.api.generated`
- **SpringDoc / Swagger UI** — available at `/swagger-ui/index.html` locally
- **Lombok** — compile-time only, excluded from Lambda JAR
- **Checkstyle** — enforced at `validate` phase; config at `checkstyle.xml` in project root

---

## Packaging Note

The Lambda JAR is built using `maven-shade-plugin` (not Spring Boot's default repackage). Spring Boot's ZIP layout puts
classes in `BOOT-INF/classes/` which Lambda's classloader cannot find. The shade plugin creates a flat uber JAR with all
classes at the root. Spring Boot repackage is disabled in `recipes-api/pom.xml`.

---

## Local Development (Tomcat)

Runs as a standard Spring Boot app with embedded Tomcat. Best for day-to-day development and IntelliJ debugging.

### IntelliJ setup

1. **Enable the Maven profile**: Maven tool window → Profiles → check `local`, then reload Maven (circular arrow icon)
2. **Set the Spring profile**: Edit Run Configuration → Spring Boot tab → Active profiles → set `local`
3. Run or debug `RecipesApplication.main()`

### How the local profile works

- **Maven `local` profile** (`recipes-api/pom.xml`): adds `spring-boot-starter-tomcat` as a dependency
- **Spring `local` profile** (`application-local.properties`): excludes `ServerlessAutoConfiguration`, which normally
  replaces Tomcat with a Lambda-only stub

Without the `local` profile, the app wires up for Lambda and cannot start as a standalone HTTP server.

### Endpoints (local Tomcat)

| Method | URL                                           | Description      |
|--------|-----------------------------------------------|------------------|
| `GET`  | `http://localhost:8080/recipes`               | List all recipes |
| `GET`  | `http://localhost:8080/recipes/{id}`          | Get recipe by ID |
| `POST` | `http://localhost:8080/recipes`               | Create a recipe  |
| `GET`  | `http://localhost:8080/swagger-ui/index.html` | Swagger UI       |
| `GET`  | `http://localhost:8080/v3/api-docs`           | OpenAPI JSON     |

---

## Local SAM Testing (Lambda emulation)

Runs the actual Lambda execution path locally in Docker via AWS SAM CLI. Use this to verify Lambda-specific behaviour
before deploying to AWS.

**Prerequisites:** Docker running, SAM CLI installed (`sam --version`), JAR built without `local` Maven profile.

### Build

```bash
# Maven 'local' profile must be OFF
mvn clean package -DskipTests
sam build
```

### Run

```bash
sam local start-api
```

API available at `http://localhost:3000`.

### Endpoints (SAM local)

| Method | URL                                  |
|--------|--------------------------------------|
| `GET`  | `http://localhost:3000/recipes`      |
| `GET`  | `http://localhost:3000/recipes/{id}` |
| `POST` | `http://localhost:3000/recipes`      |

### Cold start

The first request triggers Docker container startup + full Spring Boot context initialisation (~60s locally). Subsequent
requests on the warm container are fast. Timeout is set to 120s in `template.yaml` to accommodate this.

### Debug SAM locally

1. Start SAM in debug mode: `sam local start-api -d 5858`
2. Set a breakpoint in IntelliJ (e.g. `StreamLambdaHandler.handleRequest`)
3. Send a request to `http://localhost:3000/recipes`
4. Immediately click the debug button on the `SAM Debug` remote debug configuration in IntelliJ
5. IntelliJ connects on port 5858, JVM resumes, breakpoint is hit

**IntelliJ remote debug configuration:**

- Type: Remote JVM Debug
- Host: `localhost`
- Port: `5858`

---

## Build & Deployment

### Lambda wiring

`StreamLambdaHandler` is the Lambda handler class configured in AWS. It initialises `SpringBootLambdaContainerHandler`
once (static block) and proxies each Lambda invocation to Spring MVC.

Lambda handler configuration:

```
com.recipes.api.handler.StreamLambdaHandler::handleRequest
```

### Deploying to AWS

**Prerequisites:** AWS CLI configured (`aws configure`) with permissions for Lambda, API Gateway, CloudFormation, S3,
and IAM.

```bash
# 1. Build (Maven 'local' profile must be OFF)
mvn clean package -DskipTests

# 2. Stage the JAR for SAM (copies JAR into .aws-sam/build/ via Makefile)
sam build

# 3. First-time deploy — interactive setup, saves settings to samconfig.toml
sam deploy --guided

# 4. Subsequent deploys
sam deploy
```

`sam deploy --guided` prompts for:

- **Stack name** — e.g. `recipes-api`
- **AWS region** — e.g. `ap-southeast-2`
- **Confirm changes before deploy** — recommended `Y`
- **Allow SAM to create IAM roles** — `Y` (creates the Lambda execution role)
- **RecipesFunction may not have auth defined** — `Y` (public API, no auth)
- **Save settings to samconfig.toml** — `Y` (enables plain `sam deploy` going forward)

After deploy, SAM prints the API Gateway URL:

```
Outputs:
  RecipesFunctionApi - https://<id>.execute-api.<region>.amazonaws.com/Prod/
```

### OpenAPI code generation

API interfaces and request/response models are generated during `mvn generate-sources` from the spec at:

```
recipes-api/src/main/resources/openapi/openapi.yaml
```

Generated output lands in `recipes-api/target/generated-sources/openapi/` and is automatically added to the compile
classpath. Do not edit generated files directly.
