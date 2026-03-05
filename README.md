# Table of contents

- [What to do Task 1](#what-to-do-task-1)
- [Sub-task 1: Resource Service](#sub-task-1-resource-service)
- [Sub-task 2: Song Service](#sub-task-2-song-service)
- [Notes](#notes-task-1)
- [Checklist: Before you submit the task](#checklist-before-you-submit-the-task-1)


- [What to do Task 2](#what-to-do-task-2)
- [Sub-task 1: Dockerfiles](#sub-task-1-dockerfiles)
- [Sub-task 2: Docker Compose file](#sub-task-2-docker-compose-file)
- [Notes](#notes-task-2)
- [Checklist: Before you submit the task](#checklist-before-you-submit-the-task-2)

## What to do task 1

Your task is to implement a microservices system consisting of two services:

- **Resource Service** - for MP3 file processing
- **Song Service** - for song metadata management

### Service relationships

The services are designed to work together as follows:

- **Resource Service** handles the storage and processing of MP3 files.
- **Song Service** manages metadata for each song, ensuring that each metadata entry corresponds to a unique MP3 file in the Resource Service.
- The song metadata and resource entities maintain a one-to-one relationship:
    - Each song metadata entry is uniquely associated with a resource, linked via the resource ID.
    - Deleting a resource triggers a cascading deletion of its associated metadata.

### Requirements

- **Framework**: Spring Boot 3.4.0 or higher
- **Java Version**: Java 17 or later (LTS versions)
- **Programming Language**: Java
    - Usage of Kotlin, Groovy, Scala, or any other JVM-based language is not allowed
- **Build Tool**: Maven or Gradle
- **Database**: PostgreSQL
- **Application Startup**: In this module, Resource Service and Song Service must run locally (not in Docker)

> This course does not require creating unit tests. If you are not planning to include tests, please delete `src/test/` directory and remove the test dependencies (`spring-boot-starter-test` etc.) from your `pom.xml` or `build.gradle` files.

## Sub-task 1: Resource Service

The Resource Service implements CRUD operations for processing MP3 files. When uploading an MP3 file, the service:

- Stores the MP3 file in the database.
- Extracts the MP3 file tags (metadata) using external libraries like [Apache Tika](https://www.tutorialspoint.com/tika/tika_extracting_mp3_files.htm).
- Invokes the Song Service to save the MP3 file tags (metadata).
- Must not modify the tags (metadata) extracted from the MP3 file before sending them to the Song Service, except for converting the duration from seconds to mm:ss format.

### API endpoints

---

#### 1. Upload resource

```
POST /resources
```

**Description:** Uploads a new MP3 resource.

**Request:**

- **Content-Type:** audio/mpeg
- **Body:** Binary MP3 audio data

**Response:**

```json
{
    "id": 1
}
```

- **Description:** Returns the ID of successfully created resource.

**Status codes:**

- **200 OK** – Resource uploaded successfully.
- **400 Bad Request** – The request body is invalid MP3.
- **500 Internal Server Error** – An error occurred on the server.

---

#### 2. Get resource

```
GET /resources/{id}
```

**Description:** Retrieves the binary audio data of a resource.

**Parameters:**

- `id` (Integer): The ID of the resource to retrieve.
- **Restriction:** Must be a valid ID of an existing resource.

**Response:**

- **Body:** Returns the audio bytes (MP3 file) for the specified resource.

**Status codes:**

- **200 OK** – Resource retrieved successfully.
- **400 Bad Request** – The provided ID is invalid (e.g., contains letters, decimals, is negative, or zero).
- **404 Not Found** – Resource with the specified ID does not exist.
- **500 Internal Server Error** – An error occurred on the server.

---

#### 3. Delete resources

```
DELETE /resources?id=1,2
```

**Description:** Deletes specified resources by their IDs. If a resource does not exist, it is ignored without causing an error.

**Parameters:**

- `id` (String): Comma-separated list of resource IDs to remove.
- **Restriction:** CSV string length must be less than 200 characters.

**Response:**

```json
{
    "ids": [1, 2]
}
```

- **Description:** Returns an array of the IDs of successfully deleted resources.

**Status codes:**

- **200 OK** – Request successful, resources deleted as specified.
- **400 Bad Request** – CSV string format is invalid or exceeds length restrictions.
- **500 Internal Server Error** – An error occurred on the server.

---

## Sub-task 2: Song Service

The **Song Service** implements CRUD operations for managing song metadata records. The service uses the Resource ID to uniquely identify each metadata record, establishing a direct one-to-one relationship between resources and their metadata.

---

### API endpoints

#### 1. Create song metadata

```
POST /songs
```

**Description:** Create a new song metadata record in the database.

**Request body:**

```json
{
    "id": 1,
    "name": "We are the champions",
    "artist": "Queen",
    "album": "News of the world",
    "duration": "02:59",
    "year": "1977"
}
```

- **Description:** Song metadata fields.

**Validation rules:**

- **All fields are required.**
- `id`: Numeric, must match an existing Resource ID.
- `name`: 1-100 characters text.
- `artist`: 1-100 characters text.
- `album`: 1-100 characters text.
- `duration`: Format `mm:ss`, with leading zeros.
- `year`: `YYYY` format between 1900-2099.

**Response:**

```json
{
    "id": 1
}
```

- **Description:** Returns the ID of the successfully created metadata record (should match the Resource ID).

**Status codes:**

- **200 OK** – Metadata created successfully.
- **400 Bad Request** – Song metadata is missing or contains errors.
- **409 Conflict** – Metadata for this ID already exists.
- **500 Internal Server Error** – An error occurred on the server.

---

#### 2. Get song metadata

```
GET /songs/{id}
```

**Description:** Get song metadata by ID.

**Parameters:**

- `id` (Integer): ID of the metadata to retrieve.
- **Restriction:** Must match an existing Resource ID.

**Response:**

```json
{
    "id": 1,
    "name": "We are the champions",
    "artist": "Queen",
    "album": "News of the world",
    "duration": "02:59",
    "year": "1977"
}
```

**Status codes:**

- **200 OK** – Metadata retrieved successfully.
- **400 Bad Request** – The provided ID is invalid (e.g., contains letters, decimals, is negative, or zero).
- **404 Not Found** – Song metadata with the specified ID does not exist.
- **500 Internal Server Error** – An error occurred on the server.

---

#### 3. Delete songs metadata

```
DELETE /songs?id=1,2
```

**Description:** Deletes specified song metadata records by their IDs. If a metadata record does not exist, it is ignored without causing an error.

**Parameters:**

- `id` (String): Comma-separated list of metadata IDs to remove.
- **Restriction:** CSV string length must be less than 200 characters.

**Response:**

```json
{
    "ids": [1, 2]
}
```

- **Description:** Returns an array of the IDs of successfully deleted metadata records.

**Status codes:**

- **200 OK** – Request successful, metadata records deleted as specified.
- **400 Bad Request** – CSV string format is invalid or exceeds length restrictions.
- **500 Internal Server Error** – An error occurred on the server.

## Notes task 1

### Controllers

- Keep controllers slim; they should only handle HTTP-related concerns.
- Do not place validation (e.g., ID length checks) in controllers. Move validation to the service layer or use request DTOs with validation annotations.
- Do not include business logic (e.g., data transformations, string parsing) in controllers. Move such logic to the service layer or mappers.
- Avoid using raw entities for requests and responses to prevent exposing sensitive fields or internal schema details. Use DTOs instead.
- Wrap responses in `ResponseEntity<T>` with appropriate HTTP status codes.
- Use specific response types (e.g., `ResponseEntity<Map<String, Long>>`, `ResponseEntity<SongDto>`) to ensure API consistency.
- Controllers should not manually throw or handle exceptions. Instead, throw exceptions in the service layer and handle them in a global exception handler.

### Error Handling

- Add a global exception handler using `@RestControllerAdvice`.
- Map exceptions to appropriate HTTP status codes.
- Provide meaningful error messages and error codes in responses using a unified structure (see the [API response specification](./api-tests/api-response-specification.md) for detailed response formats):

#### Simple error response

```json
{
    "errorMessage": "Resource with ID=1 not found",
    "errorCode": "404"
}
```

#### Validation error response

```json
{
    "errorMessage": "Validation error",
    "details": {
        "duration": "Duration must be in mm:ss format with leading zeros",
        "year": "Year must be between 1900 and 2099"
    },
    "errorCode": "400"
}
```

#### Incorrect responses and why they are wrong
Example 1:
```json
{
    "errorMessage": "400 BAD_REQUEST \"Validation failure\"",
    "errorCode": 400
}
```
Issues:
- `"400 BAD_REQUEST"` in `errorMessage` is redundant (status code already exists in `errorCode`).
- No details about which fields failed validation and why.

---

Example 2:
```json
{
    "errorMessage": "Validation failure",
    "errorCode": 400
}
```
Issue:
- No details about which fields failed validation and why.

---

Example 3:
```json
{
    "errorMessage": "problemDetail.org.springframework.web.bind.MethodArgumentNotValidException",
    "errorCode": 400,
    "details": {
        "name": "Name is required"
    }
}
```
Issues:
- The `errorMessage` should never contain raw exception names (e.g., `MethodArgumentNotValidException`). This exposes internal implementation details to the API consumer.
- The message should be replaced with a human-readable `"Validation failed"`.

---

Example 4
```json
{
    "errorMessage": "Method parameter 'id': Failed to convert value of type 'java.lang.String' to required type 'int'; For input string: \"ABC\"",
    "errorCode": "400"
}
```
Issues:
- The `errorMessage` is too technical and exposes unnecessary implementation details (`java.lang.String`, `int` conversion).
- It does not clearly indicate what the user did wrong.

---


### Database implementation requirements

- Use Docker containers for database deployment.
- [PostgreSQL](https://hub.docker.com/_/postgres) 16+ is required as the database engine, preferably Alpine-based.
- Each service should have its own dedicated database instance.
- A single Docker Compose file located in the root directory of the project must be used to start both database containers.
- For this module, you can use the [provided compose.yaml](./docker-compose-file/compose.yaml) file in your project.
- The use of migration tools such as Flyway or Liquibase is not allowed.
- Database schema initialization must be fully automated using Hibernate.
- In this module, Hibernate’s `ddl-auto=update` must be used for schema management to simplify development.
- In this module, SQL initialization scripts (e.g., `schema.sql`, `data.sql`) must not be used.


<img src="images/microservice_architecture_overview.png" width="351" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

### Structure

Both microservices represent a unified application and (will) use shared files. Please merge them into a single folder (Git repository), using the following folder structure as an example:

For a Maven-based project:

```
maven-project/
├── resource-service/
│   ├── src/
│   └── pom.xml
├── song-service/
│   ├── src/
│   └── pom.xml
├── compose.yaml
└── .gitignore
```

---

For a Gradle-based project:

```
gradle-project/
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   ├── gradle-wrapper.properties
├── resource-service/
│   ├── src/
│   └── build.gradle
├── song-service/
│   ├── src/
│   └── build.gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
├── compose.yaml
└── .gitignore
```

> **Notes**:
> - The Gradle project must use the Gradle Wrapper (`gradlew`).
> - Keep `gradlew` only in the root directory.
> - Configure `settings.gradle` to include and link all services.
> - Do not ignore the Gradle Wrapper files; they must be included in the `git` repository.

---

### Postman collection and sample MP3 file for testing

Please use the [Postman collection](./api-tests/introduction_to_microservices.postman_collection.json) and a [sample MP3 file](./sample-mp3-file/mp3.zip) with the necessary tags for testing the Resource Service and Song Service APIs. This collection will help validate the correct functioning of all features and data validations. Ensure that the test results are compared against the [API response specification](./api-tests/api-response-specification.md) to verify compliance with the expected responses.

1. In the **Variables** tab of the collection, set the variables `resource_service_url` and `song_service_url` with your ports. Click **Save** to apply.

<img src="images/postman_01.png" width="1689" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

2.Send the requests. Ensure you receive the correct responses.

<img src="images/postman_02.png" width="1689" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

3. In the **Test Results** tab, verify that all tests for the requests have passed.

<img src="images/postman_03.png" width="1689" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

4. If any tests failed, make necessary adjustments to your code to ensure the API functions as expected, without changing anything in the Postman collection itself.

<img src="images/postman_04.png" width="1689" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

Ensure that your services handle **all** requests accurately and comply with the API specifications outlined in the documentation.

> **IMPORTANT!** 
> - Take screenshots confirming the successful completion of **all** API tests using provided [sample MP3 file](./sample-mp3-file/mp3.zip).
> - Compile screenshots into a **SINGLE PDF or DOCX file**, and place this file in the personal folder provided to you by Avalia.
> - Ensure the response body and test results are visible, as shown in the example below:
> <img src="images/postman_05.png" width="1000" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

### Adding a Git repository link to your personal folder

Consider placing a link to your Git repository in your personal folder for the practical task instead of uploading your files or an archive with files.

In the folder you access through the link provided by Avalia Kicker bot, add a link to your Git repository:
- Click on the "New" button.
- From the dropdown menu, select "Link".
- Paste the URL of the public Git repository with your solution.
- Save the link. The new link will now appear in the folder.

<img src="images/link_creation.png" width="500" style="border: 1px solid #ccc; margin: 20px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>

---

## Checklist: Before you submit the task 1

Before submitting your task, please ensure that you have completed all the required steps:

✅ **Controllers**
- [ ] Controllers handle only HTTP-related concerns.
- [ ] Validation logic is moved to the service layer or request DTOs with validation annotations.
- [ ] Business logic (e.g., data transformations, string parsing) is in the service layer or mappers, not in controllers.
- [ ] Raw entities are not used for requests or responses; DTOs are used instead.
- [ ] Responses are wrapped in `ResponseEntity<T>` with appropriate HTTP status codes.
- [ ] Specific response types are used (e.g., `ResponseEntity<Map<String, Long>>`, `ResponseEntity<SongDto>`) to maintain API consistency.
- [ ] Controllers do not manually throw or handle exceptions; exceptions are thrown in the service layer and handled globally.
- [ ] Endpoints strictly follow the specified paths without additional prefixes (e.g., `/api/v1/` is not added, etc.).

✅ **Error handling & validation**
- [ ] Implemented global exception handling with `@RestControllerAdvice`.
- [ ] Used the specified error response format for general and validation errors.
- [ ] Enforced all validation rules for song metadata (e.g., correct year format, duration format, required fields).

✅ **Database & Docker**
- [ ] Used PostgreSQL 16+ as the database.
- [ ] Ensured each service has its own dedicated database instance.
- [ ] Database schema initialization is fully automated.
- [ ] Used Hibernate’s `ddl-auto=update` for schema management in this module.
- [ ] Did not use migration tools such as Flyway or Liquibase.
- [ ] Did not use SQL initialization scripts (e.g., `schema.sql`, `data.sql`) in this module.
- [ ] Deployed databases in Docker containers using the [provided Docker Compose file](./docker-compose-file/compose.yaml) (`compose.yaml`).
- [ ] The Docker Compose file is located in the root directory and correctly starts both databases.
- [ ] No Dockerfiles are present, as services must run locally (not in Docker).

✅ **Project structure**
- [ ] Used the correct folder structure.
- [ ] Merged both services into a single Git repository.
- [ ] Created a public Git repository for your project.
- [ ] Excluded IDE-specific configuration files and folders (e.g., `.idea/`, `.vscode/`, `.settings/`, `*.iml`).
- [ ] Ready to place the link to your repository in the personal folder in Avalia.

✅ **API testing**
- [ ] Ran Postman tests using the provided [collection](./api-tests/introduction_to_microservices.postman_collection.json) and [sample MP3 file](./sample-mp3-file/mp3.zip).
- [ ] Verified that all API tests pass.
- [ ] Checked that all API responses conform to the [API response specification](./api-tests/api-response-specification.md).
- [ ] Took screenshots of test results and compiled them into a single PDF or DOCX file.
- [ ] Ready to place the test result PDF or DOCX file in the personal folder in Avalia.



## What to do task 2

In this module, you will adapt your services to use a containerization approach.

## Sub-task 1: Dockerfiles

1. **Create a Dockerfile for each service**. Make sure to follow these requirements:

  - Implement **two-stage builds** to create a clear separation between build and runtime environments, which helps keep the final image size small.
  - Use **Alpine images** to keep the resulting images lightweight (below are the recommended ones):
    - **Build stage**:
      - For Maven projects, use [Eclipse Temurin-based Alpine images](https://hub.docker.com/_/maven/tags?name=eclipse-temurin-17-alpine). These allow you to build Java applications efficiently while keeping the environment minimal.
      - For Gradle projects, use [Gradle Alpine images](https://hub.docker.com/_/gradle/tags?name=jdk17-alpine), designed specifically for building Java applications with Gradle.
    - **Runtime stage**:
      - Use [Eclipse Temurin Alpine images](https://hub.docker.com/_/eclipse-temurin/tags?name=17-jre-alpine) for running the application. These images include only the necessary JRE components, minimizing resource usage.
  - Introduce **dependency caching** to speed up rebuilds. This leverages Docker's layer caching to avoid re-downloading unchanged dependencies.
    - **Tips for Maven projects**:
      - Copy the `pom.xml` file before copying the source code (`src`). This allows Docker to cache dependencies if the configuration file has not changed.
      - Avoid `COPY . .` in build stage. Instead, copy files selectively to ensure Docker builds only when necessary, like `COPY src ./src`.
      - Use the command `RUN mvn dependency:go-offline` to download all dependencies before copying the source code.
    - **Tips for Gradle projects**:
      - Copy the Gradle wrapper and build configuration files (`build.gradle`, `settings.gradle`, `gradlew`) first, then install dependencies (e.g., `RUN ./gradlew dependencies --no-daemon`) This helps cache dependencies effectively.
  - **Additional tips**:
    - **General**:
      - Use `WORKDIR` to specify a consistent context for commands (e.g., `WORKDIR /app`). By using `WORKDIR`, you ensure all subsequent commands operate within a defined context without additional setup. Also, `WORKDIR` automatically creates the directory if it doesn’t already exist, so there’s no need for a separate `RUN mkdir /app` command.
      - Prefer `COPY` over `ADD` for local files, as `ADD` can introduce unexpected behavior by unpacking files or fetching URLs.
      - Avoid hardcoded JAR names by using wildcards. For example, instead of `COPY --from=build /app/target/my-application-1.0.0.jar app.jar` use `COPY --from=build /app/target/*.jar app.jar`. This way, you don’t need to update the Dockerfile if the JAR file name changes, as long as there’s only one JAR file in the target directory.
      - Use `CMD` instead of `ENTRYPOINT` to allow flexibility in overriding commands in Docker Compose or when running the container manually.
      - Use `EXPOSE` to indicate the application’s internal port in the runtime stage, e.g., `EXPOSE 8080`.
      - Avoid defining environment variables in the Dockerfile for runtime-specific values with `ARG` or `ENV`.
    - **Tips for Maven projects**:
      - Use `RUN mvn clean package -Dmaven.test.skip=true` in the Dockerfile build stage to skip both test compilation and execution for faster builds. If you want to skip running the tests but still need the test classes available, use `RUN mvn clean package -DskipTests`.
    - **Tips for Gradle projects**:
      - Include the Gradle Wrapper (`gradlew`) in your project and run all commands via `gradlew` to avoid host dependency issues. Update the `.gitignore` file to ensure that `gradlew` and `gradlew.bat` files are included in the Git repository for Docker compatibility.
      - Use the `--no-daemon` flag with `gradlew` to ensure consistent builds within Docker and manage memory usage effectively.
      - Use `RUN ./gradlew assemble --no-daemon -x test` to skip tests and speed up the Docker build. The `assemble` task compiles and packages the code without running tests by default. Adding `-x test` further ensures tests are excluded, maximizing build efficiency. This approach is faster than using `gradle build`, which includes tests by default.

2. **Test the Docker images**

  - Build Docker images for each service.
  - Run the Docker containers and **map external ports** to verify that the application starts correctly and responds to HTTP requests (e.g., using Postman).


## Sub-task 2: Docker Compose file

### 1. Container configuration

Create a `compose.yaml` (`docker-compose.yaml`) file that includes the following elements:

- **Database containers**. Make sure to follow these requirements:

  - **For each database, create a separate container** using lightweight [Alpine-based PostgreSQL images](https://hub.docker.com/_/postgres/tags?name=17-alpine) (version 16 or higher).
  - **Database-specific configurations**, such as `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`, should be read from the `.env` file.
  - **Schema management approach:**
    - In **Module 1**, schema initialization was fully automated using **Hibernate** (`ddl-auto=update`), while SQL initialization scripts (`schema.sql`, `data.sql`) were explicitly prohibited.
    - In **Module 2**, the goal is to transition to a **containerized database setup**, where schema initialization is still fully automated but must be handled via predefined SQL scripts executed within the database container, not by Spring or an ORM.
    - Using migration tools (Liquibase, Flyway, etc.) is not allowed.
    - Ensure that `spring.jpa.hibernate.ddl-auto` is set to `none` or removed in `application.properties` or `application.yml`, and that it is not passed as an environment variable in Docker Compose.
    - The following settings must not be present in `application.properties` or `application.yml` and must not be set as environment variables in Docker Compose:
      - `sql.init.mode`
      - `spring.jpa.defer-datasource-initialization`
      - `spring.flyway.enabled`
      - `spring.liquibase.enabled`, etc.
  - **Handle schema initialization externally using the database container**:
    - Schema initialization must be fully automated but executed **within the database container**.
    - SQL scripts should be placed in a dedicated directory (e.g., `init-scripts/`) and **mounted into the database container** using Docker volumes.
    - The scripts must define tables, but must NOT create the database itself.
    - The `POSTGRES_DB` environment variable in `compose.yaml` must be used to create the database automatically.

- **Microservice containers**. Ensure you comply with these requirements:

  - For each service, add a block with the `build` parameter to build images directly from the source code using the Dockerfile located in each service’s subdirectory.
  - To avoid confusion, do not use both `build` and `image` together. The `image` property is intended to pull pre-built images from a registry (e.g., Docker Hub) or assume images are manually built.
  - Specify ports (`ports`) to expose for external access.
  - Define environment variables (`environment`), including database references, using an `.env` file for variable substitution.


<img src="images/containerization_overview.png" width="351" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); display: inline-block;" alt=""/>


### 2. Microservice configuration

Be sure to meet all these conditions:

- Avoid hardcoding container-specific values (such as database URLs, credentials, service URLs, and any other configuration details specific to the containerized environment) directly in `application.properties` or `application.yml`.
- Set container-specific values as environment variables in Docker Compose.
- Use a `.env` file to define these variables, allowing Docker Compose to automatically read and inject environment-specific settings. For example:

    ```properties
    # .env
    RESOURCE_DB_URL=jdbc:postgresql://resource-db:5432/resource_db
    ```

    ```yaml
    # compose.yaml
    services:
      resource-service:
        environment:
          SPRING_DATASOURCE_URL: ${RESOURCE_DB_URL}
    ```

- Configure `application.properties` or `application.yml` to support both environment variables for containerized execution and default values for local execution (e.g., when running directly in IntelliJ). This approach ensures smooth transitions between development and deployment environments. For example::

  ```properties
  # application.properties for Resource Service
  spring.datasource.url="${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/resource_db}"
  ```

- Ensure the application can be executed both locally and in Docker Compose without requiring configuration changes or switching profiles:
  - **Local execution**:
    - Only the **database containers** should run in Docker: use a `docker compose up` command specifying the database services, e.g.: `docker compose up -d resource-db song-db`)
    - **Microservices** should run directly on the local machine (as in Module 1).
    - The application should use default values specified in `application.properties` or `application.yml` to connect to the database containers.
  - **Docker execution**:
    - Both **microservices** and **database containers** run fully in Docker Compose.
    - Docker Compose should pull configuration values from the `.env` file automatically, allowing the containerized environment to use the necessary settings without manual adjustments.


### 3. Additional notes

Adhere to the specified requirements:

- Use Docker Compose's **default network**.
- Use **logical service names** to cross-reference services for easier communication within the Docker network instead of IP addresses.
- **Persisting database data** between restarts is not enabled.


## Notes task 2

- Your configuration should automatically rebuild images, create, and start all containers, ensuring a complete service update without extra steps or scripts — all with a single command: `docker compose up -d --build`.
- Use the [Postman collection](../microservice_architecture_overview/api-tests/introduction_to_microservices.postman_collection.json) for testing the Resource Service and Song Service APIs.
- After all the changes, your project structure should look similar to this:

```
microservices/
├── init-scripts/
│   ├── resource-db/
│   │   └── init.sql
│   └── song-db/
│       └── init.sql
├── resource-service/
│   ├── src/
│   └── Dockerfile
├── song-service/
│   ├── src/
│   └── Dockerfile
├── compose.yaml
├── .env
└── .gitignore
```

---

## Checklist: Before you submit the task 2

Before submitting your task, please ensure that you have completed all the required steps:

✅ **Dockerfiles**
- [ ] Created a Dockerfile for each service following best practices.
- [ ] Used two-stage builds to separate the build and runtime environments.
- [ ] Used Alpine-based images to keep the final image size small.
- [ ] Implemented dependency caching for faster builds.
- [ ] Used `WORKDIR` to set a consistent working directory.
- [ ] Used `COPY` instead of `ADD` for local files.
- [ ] Avoided hardcoded JAR names by using wildcards.
- [ ] Used `CMD` instead of `ENTRYPOINT` to allow flexibility in overriding commands.
- [ ] Exposed the correct application port.

✅ **Docker Compose**
- [ ] Created a `compose.yaml` (`docker-compose.yaml`) file in the project root directory.
- [ ] Used Alpine-based PostgreSQL images for the databases.
- [ ] Ensured each service has its own database container.
- [ ] Moved database credentials and configuration to an `.env` file.
- [ ] Schema initialization is fully automated and handled inside the database containers, not by Spring Boot.
- [ ] Initialization scripts (`init.sql`) are not used to create databases, only tables.
- [ ] Used Docker volumes to mount initialization scripts (`init.sql`) for table creation.
- [ ] Disabled automatic tables generation (`spring.jpa.hibernate.ddl-auto` set to `none` or removed, and not defined as an environment variable in Docker Compose).
- [ ] Did not use migration tools such as Flyway or Liquibase.
- [ ] Persisting database data between restarts is not enabled (no mounted database volume).
- [ ] Used `build` instead of `image` in the microservice definitions.
- [ ] Configured services to reference each other by service names instead of IP addresses.
- [ ] Used Docker Compose's default network.

✅ **Microservice configuration**
- [ ] Removed hardcoded values (e.g., database URLs, credentials) from `application.properties`, except default ones for local execution.
- [ ] Used environment variables for all container-specific values.
- [ ] Ensured `application.properties` supports both local and Docker execution.
- [ ] Verified that services run both locally and in Docker Compose without profile switching.

✅ **Testing**
- [ ] Used the provided [Postman collection](../microservice_architecture_overview/api-tests/introduction_to_microservices.postman_collection.json) and [sample MP3 file](../microservice_architecture_overview/sample-mp3-file/mp3.zip) to test APIs both locally and in Docker Compose.
- [ ] Verified that all API requests return [correct responses](../microservice_architecture_overview/api-tests/api-response-specification.md) both locally and in Docker Compose.

✅ **Project structure**
- [ ] Ensured the final project structure follows the required format.

✅ **Final submission**
- [ ] Committed all changes to the Git repository.
- [ ] Ready to place the link to your repository in the personal folder in Avalia.
