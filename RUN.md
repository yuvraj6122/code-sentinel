# Running CodeSentinel

## Prerequisites

- Docker (with the Docker daemon / Docker Desktop running)
- Docker Compose (bundled with modern Docker as `docker compose`)

No local Java, Node, or PostgreSQL install is required — everything builds and
runs inside containers.

## Quick start (local machine)

From the project root:

```bash
docker compose up --build
```

This builds the image, starts PostgreSQL, waits until the database is healthy,
then launches the app.

## Links

Once the containers are up, the app is available at:

| Service   | URL                                      |
| --------- | ---------------------------------------- |
| Frontend  | http://localhost:8080                    |
| Backend   | http://localhost:8080/api                |
| Health    | http://localhost:8080/api/health         |

The frontend and backend share the same origin (port 8080): nginx serves the
UI and forwards every `/api/*` request to the Spring Boot backend, so there are
no CORS or separate-port concerns.

### API endpoints (all under `/api`)

| Method | Path                        | Purpose                       |
| ------ | --------------------------- | ----------------------------- |
| GET    | `/api/health`               | Health check                  |
| POST   | `/api/repositories/analyze` | Repository metadata scan      |
| POST   | `/api/duplication/analyze`  | Duplicate-code analysis       |
| POST   | `/api/complexity/analyze`   | Complexity analysis           |
| POST   | `/api/testing/analyze`      | Testing-quality analysis      |
| POST   | `/api/security/analyze`     | Security analysis             |

## Build steps

The `Dockerfile` is a multi-stage build:

1. **Frontend stage** (`node:20-slim`) — installs dependencies with `npm ci`
   and builds the static assets with `npm run build`.
2. **Backend stage** (`eclipse-temurin:21-jdk`) — builds the Spring Boot fat
   jar with `./gradlew bootJar`.
3. **Runtime stage** (`eclipse-temurin:21-jdk`) — installs nginx, supervisor,
   and the build tools (maven, gradle, git) needed to compile cloned repos,
   copies in the jar and the built frontend, and runs both processes under
   supervisord.

Build the image on its own (without compose):

```bash
docker build -t codesentinel:latest .
```

> The **first build is slow** (npm build + Gradle downloading its distribution
> + apt installing the JDK/maven/gradle toolchain). Subsequent builds are
> cached and fast. After changing source code, re-run with `--build`.

## Configuration

Everything is overridable via environment variables (or a `.env` file next to
`docker-compose.yml`):

| Variable            | Default            | Description                         |
| ------------------- | ------------------ | ----------------------------------- |
| `APP_PORT`          | `8080`             | Host port mapped to container `80`  |
| `POSTGRES_DB`       | `codesentinel`     | Database name                       |
| `POSTGRES_USER`     | `codesentinel_user`| Database user                       |
| `POSTGRES_PASSWORD` | `codesentinel`     | Database password                   |

Example — run on a different host port:

```bash
APP_PORT=3000 docker compose up --build
```