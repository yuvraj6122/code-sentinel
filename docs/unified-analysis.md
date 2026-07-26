# Unified Analysis Orchestrator

The Unified Analysis Orchestrator turns the independent analysis agents into a
single pipeline. One request clones a repository, scans it, runs every analysis
agent, aggregates and persists the findings under a single analysis record, and
returns a repository-level summary.

It answers:

> What is the overall state of this repository across all agents, in one call?

## Lifecycle

```
GitHub Repository URL
        |
        v
Repository Clone (once, cached per url@commit)
        |
        v
Repository Scanner (best-effort metadata)
        |
        v
Persist RepositoryEntity + create Analysis (RUNNING)
        |
        +----------------+----------------+----------------+
        v                v                v                v
   Complexity       Duplicate         Testing          Security
     Agent            Agent            Agent             Agent
        |                |                |                |
        +----------------+----------------+----------------+
                         |
                         v
              Aggregate all findings
                         |
                         v
        Persist findings under the one Analysis
                         |
                         v
              Mark Analysis COMPLETED
                         |
                         v
              UnifiedAnalysisResponse
```

## Design

The orchestrator **coordinates** the workflow; it does not reimplement any agent.
It reuses the existing building blocks directly:

| Concern | Reused component |
| ------- | ---------------- |
| Clone | `RepositoryCloneService` (already caches a clone per `url@commit`) |
| Scan | `RepositoryScannerAgent` |
| Analysis | `ComplexityAnalysisAgent`, `DuplicateCodeAnalysisAgent`, `TestingAnalysisAgent`, `SecurityAnalysisAgent` |
| Persistence | `RepositoryEntity`/`Analysis`/`Finding` models + `RepositoryEntityRepository`, `AnalysisRepository`, `FindingRepository` |
| Reporting | `FindingDto` and the shared `Response.from(...)` / severity-count convention |

### One analysis per run

The per-agent services (`ComplexityAnalysisServiceImpl`, etc.) each create their
own `RepositoryEntity` and `Analysis`, so calling all four independently produces
four separate analysis records. The orchestrator instead calls the **agents**, so
a run produces **one** `RepositoryEntity` and **one** `Analysis` with every
agent's findings linked to it. The per-agent services and endpoints remain in
place and unchanged for backward compatibility.

### Class responsibilities

- `UnifiedAnalysisService` / `UnifiedAnalysisServiceImpl` — the orchestration
  logic, kept as small, single-purpose helpers (`scanRepository`,
  `persistRepository`, `startAnalysis`, `runAgents`, `runAgent`,
  `persistFindings`, `completeAnalysis`) rather than one monolithic method.
- `AgentExecutionResult` — internal per-agent outcome (agent type, success flag,
  findings, error message). Keeps a failure from propagating out of an agent.
- `UnifiedAnalysisResponse` / `AgentExecutionDto` — the API response and its
  per-agent execution status.
- `AnalysisController` — exposes `POST /api/analysis/analyze`.

## Failure handling

The pipeline is resilient by design. Each agent runs inside its own try/catch
(`runAgent`): if an agent throws, the orchestrator

- logs the failure (with the stack trace) and the message,
- records that agent as `FAILED` in the result,
- continues running the remaining agents,
- persists the findings from the agents that succeeded,
- returns a **partial** result.

Because the agents only read the filesystem (they never write to the database),
catching their exceptions inside the surrounding `@Transactional` method is safe
and does not poison the transaction. The repository scan is likewise best-effort:
if it fails, the run proceeds with repository metadata derived from the URL.

Only genuinely fatal, pre-analysis problems abort the request: an invalid URL or
a failed clone surface through the existing `GlobalExceptionHandler`
(`InvalidRepositoryUrlException` → 400, `RepositoryCloneException` → 502).

The run-level `status` reflects agent outcomes:

| status | meaning |
| ------ | ------- |
| `COMPLETED` | every agent succeeded |
| `PARTIAL` | at least one agent succeeded and at least one failed |
| `FAILED` | every agent failed (the record is still persisted) |

The persisted `Analysis.status` is `COMPLETED` once the workflow finishes; the
per-agent detail lives in the response so partial results are transparent.

## Aggregation

Summary metrics are derived from the persisted findings (no schema change
required):

- `totalFindings`
- severity counts: `criticalSeverity`, `highSeverity`, `mediumSeverity`,
  `lowSeverity`
- `findingsByCategory` — counts keyed by `COMPLEXITY`, `DUPLICATE_CODE`,
  `TESTING`, `SECURITY`
- `agentExecutions` — per-agent status + finding contribution + failure message
- `metadata` — repository metadata (`repositoryName`, `language`, `buildTool`,
  `javaFileCount`, `testFileCount`) from the scanner, so the dashboard's
  repository overview renders from this response
- `testingMetrics` — the Testing Analysis Agent's maturity summary (frameworks,
  integration status, class/method/disabled counts, and the 0-100 score); null if
  the testing agent did not complete

The flat `findings` list carries each finding's `agentType` and `severity`, so a
client can derive every per-category section (findings + severity counts) from
this single response without calling the per-agent endpoints. The dashboard does
exactly this — it runs one unified analysis and renders all sections from the
result, and the Planning Agent later reuses the same `analysisId`.

## API

`POST /api/analysis/analyze`

Request (reuses `CloneRepositoryRequest`):

```json
{ "githubUrl": "https://github.com/owner/repo" }
```

Response (`UnifiedAnalysisResponse`):

```json
{
  "analysisId": 1,
  "repository": "https://github.com/owner/repo",
  "repositoryName": "repo",
  "metadata": {
    "repositoryName": "repo",
    "language": "JAVA",
    "buildTool": "GRADLE",
    "javaFileCount": 120,
    "testFileCount": 34
  },
  "status": "PARTIAL",
  "totalFindings": 25,
  "criticalSeverity": 0,
  "highSeverity": 5,
  "mediumSeverity": 12,
  "lowSeverity": 8,
  "findingsByCategory": {
    "COMPLEXITY": 9,
    "DUPLICATE_CODE": 4,
    "TESTING": 12,
    "SECURITY": 0
  },
  "agentExecutions": [
    { "agent": "COMPLEXITY", "status": "COMPLETED", "findings": 9, "message": null },
    { "agent": "DUPLICATE_CODE", "status": "COMPLETED", "findings": 4, "message": null },
    { "agent": "TESTING", "status": "COMPLETED", "findings": 12, "message": null },
    { "agent": "SECURITY", "status": "FAILED", "findings": 0, "message": "..." }
  ],
  "findings": [ /* FindingDto[] across all agents */ ],
  "testingMetrics": {
    "testingFrameworks": ["JUnit 5"],
    "mockingFrameworks": ["Mockito"],
    "hasTests": true,
    "hasIntegrationTests": false,
    "testClassCount": 30,
    "testMethodCount": 210,
    "disabledTestCount": 3,
    "maturityScore": 72,
    "maturitySummary": "Solid unit coverage; add integration tests"
  }
}
```

The individual per-agent endpoints (`/api/complexity/analyze`,
`/api/duplication/analyze`, `/api/testing/analyze`, `/api/security/analyze`) and
the scanner endpoint (`/api/repositories/analyze`) are unchanged and continue to
work for callers that want a single agent.

## Logging

The workflow logs each stage so a run can be followed end-to-end:

```
Starting unified repository analysis for <url>
Running repository scan...
Repository scan complete — <name> (<build> build), <n> java files (<t> tests)
Running Complexity Agent...
Complexity Agent completed — <n> findings
Running Duplicate Code Agent...
...
Security Agent failed — <message>. Continuing with the remaining agents.
Unified analysis #<id> complete — <n> findings across 4 agents (1 failed)
```

## Testing

- `UnifiedAnalysisResponseTest` — aggregation and summary calculations: severity
  counts (including critical), category grouping, per-agent execution mapping,
  and `COMPLETED`/`PARTIAL`/`FAILED` status resolution.
- `UnifiedAnalysisServiceImplTest` — the orchestration workflow: the successful
  path (all agents run, findings aggregated under a single analysis that is then
  completed), the partial-failure path (one agent throws, the rest still run and
  persist, result is `PARTIAL`, the run still completes), and graceful handling
  of a failed repository scan.
