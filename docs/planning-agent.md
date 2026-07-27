# AI Planning Agent

The Planning Agent interprets the evidence collected by the analysis agents and
produces a prioritized set of engineering recommendations plus an overall
repository-health assessment. Unlike the analysis agents, it **does not analyze
source code** — it reads the findings already persisted for an analysis and asks
OpenAI to reason over them.

The design rationale (purpose, inputs/outputs, prompt strategy, sequence diagram,
and decisions) lives in [planning-agent-design.md](planning-agent-design.md). This
document describes the shipped implementation (Issue #25).

## Where it fits

```
Analysis (findings persisted by the Unified Analysis Orchestrator)
        │
        ▼
Planning Agent  ──►  PromptBuilder  ──►  OpenAiClient  ──►  OpenAI
        │                                                     │
        │◄──────────────── structured JSON ───────────────────┘
        ▼
RecommendationValidator  ──►  persist (Recommendation + assessment)  ──►  REST API  ──►  Frontend
```

It runs independently of the analysis pipeline, so an OpenAI or validation
failure can never crash or roll back an analysis.

## Components

| Component | Responsibility |
| --- | --- |
| `PlanningAgentService` / `PlanningAgentServiceImpl` | Orchestrates: load analysis + findings, build prompt, call OpenAI, validate, persist, and return the report. Idempotent per analysis. |
| `PromptBuilder` | Turns repository metadata + findings into a concise system + user prompt. Sends aggregate counts plus a bounded, severity-ranked sample of findings per category (`max-findings-per-category`). |
| `OpenAiClient` / `OpenAiClientImpl` | JDK `HttpClient` call to OpenAI chat-completions in JSON-object mode, with per-request timeout and linear-backoff retries on transient failures (timeouts, 429, 5xx). Reads the API key from configuration. |
| `RecommendationValidator` | Validates and normalizes the model output. Rejects structurally invalid responses (missing assessment, missing required fields, invalid impact) so nothing invalid is persisted; drops unknown categories and defaults non-positive priorities. |
| `Recommendation` (entity) + `RecommendationRepository` | Persistence, associated with an `Analysis`. |
| `RecommendationController` | REST endpoints. |

## Prompt & response

The system prompt casts the model as a senior engineer/tech lead, instructs it to
interpret (not re-analyze) the evidence, prioritize by risk, merge related
findings, ground everything in the provided findings, and respond with **JSON
only**. The expected schema:

```json
{
  "overallAssessment": "string",
  "recommendations": [
    {
      "priority": 1,
      "title": "string",
      "description": "string",
      "reason": "string",
      "impact": "HIGH | MEDIUM | LOW",
      "affectedCategories": ["SECURITY", "COMPLEXITY", "TESTING", "DUPLICATE_CODE"]
    }
  ]
}
```

`priority` is a 1-based integer (1 = most important). `impact` maps to the
`RecommendationImpact` enum. `affectedCategories` map to `AgentType`; unknown
values are dropped during validation.

## Recommendation lifecycle

1. A repository is analyzed (e.g. via the Unified Analysis Orchestrator), which
   persists all findings under one `Analysis`.
2. `POST /api/analyses/{analysisId}/recommendations` triggers generation.
3. If recommendations already exist and `force` is false, the stored set is
   returned without calling OpenAI. With `force=true`, prior recommendations are
   cleared and regenerated.
4. The prompt is built, OpenAI is called, and the JSON response is parsed and
   validated. An invalid response is rejected with no partial writes.
5. Valid recommendations are persisted (each linked to the analysis) and the
   assessment is stored on the analysis; the report is returned.

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/analyses/{analysisId}/recommendations?force=false` | Generate recommendations. Returns the stored set unless `force=true`. |
| `GET` | `/api/analyses/{analysisId}/recommendations` | Return the stored recommendations and assessment. |

Response body (`RecommendationReportResponse`):

```json
{
  "analysisId": 42,
  "overallAssessment": "…",
  "generatedAt": "2026-07-25T12:00:00",
  "totalRecommendations": 2,
  "recommendations": [
    {
      "priority": 1,
      "title": "…",
      "description": "…",
      "reason": "…",
      "impact": "HIGH",
      "affectedCategories": ["SECURITY"]
    }
  ]
}
```

A missing analysis returns `404`; OpenAI/validation failures return `502` with a
message (via `GlobalExceptionHandler`).

## Configuration

All knobs live under `codesentinel.planning.*` in `application.properties`. The
API key is read from the environment and never hard-coded:

```properties
codesentinel.planning.api-key=${OPENAI_API_KEY:}
codesentinel.planning.model=gpt-4o-mini
codesentinel.planning.base-url=https://api.openai.com/v1
codesentinel.planning.timeout-seconds=60
codesentinel.planning.max-retries=2
codesentinel.planning.retry-backoff-millis=1000
codesentinel.planning.temperature=0.2
codesentinel.planning.max-findings-per-category=15
codesentinel.planning.max-recommendations=7
```

## Error handling

`PlanningAgentException` covers a missing/blank API key, exhausted retries after
transient failures (timeouts, 429, 5xx), non-recoverable API errors (e.g. 401),
malformed JSON, and structurally invalid responses. An empty `recommendations`
array is a valid outcome (the assessment is persisted with no actions). Because
generation is a dedicated endpoint separate from the analysis pipeline, these
failures never affect stored analyses.

## Frontend

The dashboard runs a single **Unified Analysis Orchestrator** request
(`POST /api/analysis/analyze`) and derives every section from that one response —
repository overview, complexity, duplicate code, testing (including maturity
metrics), and security — so all agents run once under a single `analysisId`.

The **AI Recommendations** section (`RecommendationsSection`) offers a "Generate AI
Recommendations" action that operates on that existing `analysisId` via
`POST /api/analyses/{analysisId}/recommendations`. It never triggers a second
repository analysis. Results render as the overall assessment plus prioritized
cards (priority, title, description, reason, impact) with affected-category chips,
reusing the existing dashboard components and styling.

## Testing

Unit tests mock the OpenAI boundary (`OpenAiClient`) — no live API calls:

- `PromptBuilderTest` — prompt content, severity/category breakdown, and per-category budgeting.
- `RecommendationValidatorTest` — normalization and rejection of invalid responses.
- `PlanningAgentServiceImplTest` — generation, validation, persistence, retrieval, idempotency, and malformed/empty-response handling.
