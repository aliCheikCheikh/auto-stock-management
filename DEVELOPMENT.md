# Local backend development

The backend does not select a Spring profile by default. In production, incomplete
configuration must prevent startup rather than load development settings and a
bootstrap account.

## Prerequisites

- Java 17
- Maven
- Docker with the engine running

## Configure the environment

From the repository root, copy `.env.example` to `.env` and fill in its required
values. Keep `.env` local and do not commit it.

Docker Compose reads `.env` automatically. Export the same values before running
Java from your terminal; Spring Boot does not automatically read this file:

```bash
set -a
. ./.env
set +a
```

## Start PostgreSQL

```bash
docker compose up -d postgres
docker compose ps
```

Wait until `auto-stock-postgres` is `healthy` before starting the application.

## Build and run from a terminal

From the repository root, build all modules before starting the packaged application:

```bash
mvn -DskipTests package
java -jar stock-infrastructure/target/stock-infrastructure-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

This command skips test execution. Run `mvn verify` separately with Docker running
to execute the full test suite, including container-based integration tests.

Successful startup logs include:

```text
The following 1 profile is active: "dev"
HikariPool-1 - Start completed.
Tomcat started on port 8080
```

Flyway also reports migration validation and applies any pending migrations.

## Run from IntelliJ IDEA

Select the shared **Local backend — dev** configuration and provide the environment
values from `.env` in its environment settings before clicking Run. It explicitly
sets `SPRING_PROFILES_ACTIVE=dev` and uses the `stock-infrastructure` module.

A temporary configuration generated from `StockApplication.main()` does not activate
the development profile automatically.

## Troubleshooting

- `Failed to configure a DataSource: 'url' attribute is not specified`: check that the
  `dev` profile is active and the required configuration is present.
- `Connection refused` on port `5432`: PostgreSQL is stopped or not yet `healthy`.
- Flyway error: do not modify a migration that has already been applied. Compare the
  reported version and checksum before making changes.
