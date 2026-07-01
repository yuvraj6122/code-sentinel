# Development Workflow

This document describes the engineering practices used to build CodeSentinel:
version control, issue tracking, code review, and automated testing. It is the
canonical reference for contributors.

## Branch Strategy

CodeSentinel uses a lightweight trunk-based model built around short-lived
feature branches.

- **`main`** is the protected, always-releasable trunk. It is never committed to
  directly; all changes arrive via reviewed pull requests.
- **Feature branches** are created from the latest `main` for each unit of work.
  Use short, descriptive names that reflect the change, for example:
  - `repo-scanner`
  - `complexity`
  - `database-model`
- Keep branches small and focused on a single logical concern so they are easy
  to review and quick to merge.
- Rebase or merge the latest `main` into your branch before opening or updating a
  pull request to keep it current and minimize conflicts.
- Delete the branch after the pull request is merged.

## Issue Tracking Process

Work is planned and tracked with **GitHub Issues**.

1. **Create an issue** for every feature, bug, or task before starting work.
   Describe the problem, the desired outcome, and acceptance criteria.
2. **Label and organize** issues (e.g. `feature`, `bug`, `docs`) so the backlog
   is easy to scan and prioritize.
3. **Assign** the issue to the person picking it up.
4. **Reference the issue** from the branch and pull request. Use closing keywords
   such as `Closes #123` in the PR description so the issue is closed
   automatically on merge.
5. **Track progress** by moving the issue through its lifecycle (open → in
   progress → in review → closed).

## Pull Request Workflow

Every change reaches `main` through a pull request.

1. Create a feature branch from `main`.
2. Implement the change with clear, incremental commits.
3. Run the build and tests locally (`./gradlew build` for the backend).
4. Push the branch and open a pull request against `main`.
5. Fill out the [pull request template](../.github/pull_request_template.md):
   - **Summary** — what changed and why.
   - **Related Issue** — the issue this PR resolves.
   - **Testing Performed** — how the change was verified.
   - **Checklist** — build, tests, and docs confirmation.
6. Wait for **CI to pass**. Continuous integration runs automatically on the PR.
7. Address **code review** feedback. At least one approving review is required
   before merge.
8. **Merge** once CI is green and the PR is approved (squash or merge commit),
   then delete the feature branch.

### Code Review Expectations

- Reviewers check correctness, readability, test coverage, and adherence to
  project conventions.
- Authors respond to all comments and re-request review after changes.
- Prefer small PRs — they get reviewed faster and more thoroughly.

## CI Pipeline Overview

Continuous integration is implemented with **GitHub Actions**. Workflows live in
`.github/workflows/` and run on every `push` to `main` and every `pull_request`,
so code quality is validated before merge.

### Backend CI — `.github/workflows/backend-ci.yml`

- Triggers on changes under `backend/**` (and to the workflow file itself).
- Steps:
  1. Check out the repository.
  2. Set up **Java 21** (Temurin).
  3. Set up **Gradle** with dependency and build caching
     (`gradle/actions/setup-gradle`) for faster runs.
  4. Start a **PostgreSQL 16** service container. The full-context
     `@SpringBootTest` requires a reachable database; credentials mirror
     `backend/src/main/resources/application.properties`.
  5. Run `./gradlew build`, which compiles the code and executes all tests.
  6. Upload test reports as a build artifact.
- The workflow fails if the build or any test fails, which blocks the merge.

### Frontend CI — `.github/workflows/frontend-ci.yml`

- Triggers on changes under `frontend/**` (and to the workflow file itself).
- Steps:
  1. Check out the repository.
  2. Detect whether the frontend has been scaffolded (a `frontend/package.json`).
     Until it exists, the job skips gracefully and stays green.
  3. Set up **Node.js 20** with **npm caching**.
  4. Install dependencies (`npm ci` when a lockfile is present, otherwise
     `npm install`).
  5. Build the frontend (`npm run build`).
- The workflow fails if the frontend build fails.

### Performance & Caching

- **Gradle cache** is restored/saved by `gradle/actions/setup-gradle`, avoiding
  repeated dependency downloads and speeding up incremental builds.
- **npm cache** is restored by `actions/setup-node` keyed on the lockfile.
- **Concurrency groups** cancel superseded runs on the same branch to save CI
  minutes.
- **Path filters** ensure backend changes don't trigger frontend CI and vice
  versa.

## Automated Testing

- Backend tests use **JUnit 5** and run via `./gradlew test` (included in
  `./gradlew build`).
- Tests run locally before opening a PR and automatically in CI on every PR.
- A failing test fails CI and prevents the change from being merged, keeping
  `main` healthy.

## Summary

| Practice          | Tooling                                   |
| ----------------- | ----------------------------------------- |
| Version control   | Git + GitHub, trunk-based feature branches |
| Issue tracking    | GitHub Issues                             |
| Code review       | GitHub Pull Requests + required review    |
| Automated testing | JUnit 5 via Gradle, run in GitHub Actions |
| CI/CD             | GitHub Actions (backend + frontend)       |
