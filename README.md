# ToDo List

```
Test Assesment
```

#### RESTful API service for operation with ToDo items.

Rest API is build on top of SpringMVC framework with API generation from OpenAPI schema.

### Requirements:
- Docker

### Test Requirements:
- JDK 14
- Gradle

### Language used:
- Kotlin v1.5

## HowTo's

### Run Tests:

```
./gradlew test
```

### Build and Dockerize:
```
 docker build -t todos_api . 
```

### Run Docker image:

```
docker run --rm -p 8080:8080 todos_api
```
_**--rm** flag could be removed in order to not remove image after execution_

### Custom Gradle tasks:

- _generateSchemaApis_ - Generate Domain structure, Controller implementation and Service Layer interfaces out of OpenAPI schema
- _generateJooq_ - Generate Repository Layer

### Libraries:

#### Runtime
- *Jooq* - Plugin is used for Repository layer generation. Runtime library used for building compile-time validated declarative SQL queries.
- *Springdoc* - Swagger-UI implementation
#### Test
- *Springmockk* - Kotlin wrapper for Mockito library
- *Awaility* - Async execution validation library

## API Access

Api schema, exposed by Swagger-UI, could be observed after server startup from root context path ``/``
```
E.g.: http://localhost:8080/
```