# CodeSentinel

[![Backend CI](https://github.com/yuvraj6122/code-sentinel/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/yuvraj6122/code-sentinel/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/yuvraj6122/code-sentinel/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/yuvraj6122/code-sentinel/actions/workflows/frontend-ci.yml)

CodeSentinel is a multi-agent AI platform for code quality analysis and technical debt management.

## Overview

The system analyzes Java repositories and generates insights regarding:

- Code complexity
- Security vulnerabilities
- Testing quality
- Duplicate code
- Technical debt

## Planned Architecture

- Repository Scanner Agent
- Complexity Analysis Agent
- Security Analysis Agent
- Testing Analysis Agent
- Duplicate Code Analysis Agent
- Planning Agent

## Technology Stack

### Backend
- Java 21
- Spring Boot

### Frontend
- React
- TypeScript

### Database
- PostgreSQL

### AI
- LangGraph
- LangChain
- OpenAI API

### Cloud
- AWS EC2
- AWS RDS
- AWS S3

## Getting Started

### Prerequisites

- Java 21 (Temurin recommended)
- PostgreSQL 16 (a database named `codesentinel`)
- Git

### Clone the repository

```bash
git clone https://github.com/yuvraj6122/code-sentinel.git
cd code-sentinel
```

### Set up the database

The backend expects a PostgreSQL database. Defaults live in
`backend/src/main/resources/application.properties` and can be overridden with
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` environment variables.

```bash
createdb codesentinel
createuser codesentinel_user --pwprompt   # password: codesentinel
```

### Run the backend

```bash
cd backend
./gradlew bootRun          # start the app
./gradlew build            # compile, run tests, and package
```

The API is served at `http://localhost:8080`.

### Frontend

The frontend (React + TypeScript) lives under `frontend/`. Once it is scaffolded
with a `package.json`, run:

```bash
cd frontend
npm install
npm run build
```

## Development Workflow

CodeSentinel follows standard collaborative engineering practices. A detailed
guide is available in [docs/development-workflow.md](docs/development-workflow.md).

### GitHub Issues

- Work is tracked as GitHub Issues (features, bugs, and tasks).
- Each issue captures the problem, acceptance criteria, and relevant context.
- Branches and pull requests reference the issue they resolve (e.g. `Closes #123`).

### Pull Requests

- All changes land on `main` through pull requests — no direct pushes to `main`.
- Open PRs from a short-lived feature branch (e.g. `repo-scanner`, `complexity`).
- PRs use the repository [pull request template](.github/pull_request_template.md)
  and require at least one review before merge.
- CI must be green before a PR is merged.

### GitHub Actions CI

Continuous integration runs automatically on every `push` to `main` and on every
`pull_request`:

- **Backend CI** (`.github/workflows/backend-ci.yml`): sets up Java 21, provisions
  a PostgreSQL service, restores the Gradle cache, then builds and runs all tests.
- **Frontend CI** (`.github/workflows/frontend-ci.yml`): sets up Node.js, restores
  the npm cache, installs dependencies, and builds the frontend.

A failing build or failing test fails the workflow and blocks the merge.

### Automated Testing

- Backend tests run under JUnit 5 via `./gradlew build` (or `./gradlew test`).
- Tests execute in CI on every pull request, so regressions are caught before merge.
- Test reports are uploaded as CI artifacts for inspection.

## Status

Project currently under development.