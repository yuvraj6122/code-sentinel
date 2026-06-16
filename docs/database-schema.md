# Database Schema

Initial MVP schema for CodeSentinel. Hibernate creates and updates tables automatically via `spring.jpa.hibernate.ddl-auto=update`.

## Entity Diagram

```
Repository
    |
    +---- Analysis
              |
              +---- Finding
```

## Tables

### repositories

Represents a GitHub repository submitted for analysis.

| Column      | Type          | Notes                          |
|-------------|---------------|--------------------------------|
| id          | BIGINT (PK)   | Auto-generated                 |
| github_url  | VARCHAR       | e.g. `https://github.com/...`  |
| name        | VARCHAR       | Repository name                |
| language    | VARCHAR       | e.g. `Java`                    |
| build_tool  | VARCHAR       | e.g. `Gradle`                  |
| created_at  | TIMESTAMP     | When the repository was added  |

### analyses

Created each time a user clicks **Analyze** on a repository.

| Column        | Type          | Notes                                      |
|---------------|---------------|--------------------------------------------|
| id            | BIGINT (PK)   | Auto-generated                             |
| repository_id | BIGINT (FK)   | References `repositories.id`               |
| status        | VARCHAR       | `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`|
| started_at    | TIMESTAMP     | When analysis began                        |
| completed_at  | TIMESTAMP     | When analysis finished (nullable)          |

### findings

Individual issues reported by an analysis agent.

| Column      | Type          | Notes                                                |
|-------------|---------------|------------------------------------------------------|
| id          | BIGINT (PK)   | Auto-generated                                       |
| analysis_id | BIGINT (FK)   | References `analyses.id`                             |
| agent_type  | VARCHAR       | e.g. `SECURITY`, `COMPLEXITY`, `TESTING`             |
| severity    | VARCHAR       | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`                  |
| title       | VARCHAR       | Short summary                                        |
| description | TEXT          | Detailed explanation                                 |
| file_path   | VARCHAR       | e.g. `UserController.java`                           |

## Relationships

- **Repository → Analysis** — one-to-many (`repositories.id` → `analyses.repository_id`)
- **Analysis → Finding** — one-to-many (`analyses.id` → `findings.analysis_id`)

## Out of Scope (MVP)

The following are intentionally not modeled yet:

- Users
- Teams
- Organizations
- Projects
- Authentication
