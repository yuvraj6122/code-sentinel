# Planning Agent — Architecture & Design

Related docs: [Architecture](architecture.md) ·
[Unified Analysis Orchestrator](unified-analysis.md) ·
[Complexity](duplicate-code-analysis.md) · [Testing](testing-analysis.md) ·
[Security](security-analysis.md)

---

## 1. Purpose

CodeSentinel's existing agents are **evidence collectors**. Each one runs a
proven tool over a cloned repository and emits normalized `Finding`s:

- Repository Scanner → metadata (language, build tool, file counts)
- Complexity Agent (PMD) → complexity/length findings
- Duplicate Code Agent (PMD CPD) → duplication findings
- Testing Agent (JavaParser) → test-quality findings + a maturity score
- Security Agent (SpotBugs + FindSecBugs) → security findings

What they **don't** do is answer the question a human actually cares about:

> "Given everything you found, what should I fix first, and why?"

A repository can produce dozens or hundreds of findings across four agents.
Findings are individually true but collectively overwhelming, un-prioritized, and
lacking cross-cutting context (e.g. "this class is both highly complex *and*
untested *and* has a security smell — fix it first").

The **Planning Agent** exists to **interpret** the collected evidence and produce
a small, prioritized set of actionable engineering recommendations, plus an
overall assessment of the repository's health.

| | Analysis Agents | Planning Agent |
| --- | --- | --- |
| Role | Collect evidence | Interpret evidence |
| Input | Cloned source / bytecode | Existing `Finding`s + metadata |
| Engine | Static-analysis tools (PMD, SpotBugs, JavaParser) | LLM (OpenAI) |
| Output | Many `Finding`s (facts) | Few `Recommendation`s (judgment) |
| Determinism | Deterministic | Non-deterministic (LLM) |
| Runs | During analysis, on the filesystem | After analysis, on persisted data |

**One-line framing:** *Analysis agents tell you what is wrong; the Planning Agent
tells you what to do about it.*

---

## 2. Position in the Architecture

The Planning Agent is a **post-analysis** consumer. It runs only once the unified
analysis has completed and its `Finding`s are persisted. It does **not** touch the
cloned repository and is **not** one of the four analysis agents run by
`UnifiedAnalysisServiceImpl.runAgents(...)`.

```
                         Repository (GitHub URL)
                                  │
                                  ▼
                        Repository Clone Service
                                  │
                                  ▼
                          Repository Scanner
                                  │
              ┌───────────┬───────┴───────┬────────────┐
              ▼           ▼               ▼            ▼
         Complexity   Duplicate        Testing     Security     (evidence)
              └───────────┴───────┬───────┴────────────┘
                                  ▼
                    Unified Analysis Orchestrator
             (persist Finding[] under one Analysis record)
                                  │
                                  ▼   ← NEW, this design
                            Planning Agent
                    (reads persisted findings + metadata)
                                  │
                                  ▼
                              OpenAI API
                                  │
                                  ▼
                      Recommendation[] + assessment
                          (persisted, new tables)
                                  │
                                  ▼
                           REST API  →  Frontend
```

Key architectural placement notes:

- The existing pipeline is unchanged. The Planning Agent is an **additive layer**
  that reads what the orchestrator already persisted (`Analysis` + `Finding`s).
- `AgentType.PLANNING` **already exists** in the `AgentType` enum, so the concept
  is already reserved in the model.
- Because it consumes persisted findings (keyed by `analysisId`), planning can be
  triggered either **synchronously at the end of the unified run** or **on demand
  for an existing analysis** — see §8 and §11.

---

## 3. Inputs

The Planning Agent consumes the **outputs of the analysis pipeline**, not source
code. Every input is already produced and persisted today.

| Input | Source (current code) | Notes |
| --- | --- | --- |
| Repository metadata | `RepositoryEntity` (`name`, `githubUrl`, `language`, `buildTool`) ← `RepositoryScannerAgent` / `RepositoryMetadataDto` | Identifies and describes the repo. `javaFileCount`/`testFileCount` are on `RepositoryMetadataDto`. |
| Analysis metadata | `Analysis` (`id`, `status`, `startedAt`, `completedAt`) | `status` may be `COMPLETED` or `PARTIAL`; the Planning Agent must know if some agents failed (incomplete evidence). |
| Aggregated summary | `UnifiedAnalysisResponse` (`totalFindings`, severity counts, `findingsByCategory`, `agentExecutions`) | Cheap, high-signal context ideal for the prompt. `agentExecutions` reveals which agents failed/were skipped. |
| Complexity findings | `Finding` where `agentType = COMPLEXITY` | title/description/filePath (+ optional lineNumber). |
| Security findings | `Finding` where `agentType = SECURITY` | Includes `lineNumber`; typically highest priority. |
| Testing findings | `Finding` where `agentType = TESTING` | Plus the testing maturity score (from `TestingMetrics`) if surfaced. |
| Duplicate findings | `Finding` where `agentType = DUPLICATE_CODE` | Duplication hot-spots. |

Each `Finding` (see `model/Finding.java`) provides:

```
agentType : AgentType      // COMPLEXITY | SECURITY | TESTING | DUPLICATE_CODE
severity  : Severity       // LOW | MEDIUM | HIGH | CRITICAL
title     : String
description: String
filePath  : String  (nullable)
lineNumber: Integer (nullable)
```

**Input-shaping concern (for §5/§11):** a repo can have hundreds of findings.
The prompt must **not** dump all of them (token cost + noise). The design calls
for a *summarized* input: the aggregated counts (`findingsByCategory`, severity
counts) plus a bounded, severity-ranked sample of representative findings per
category. The exact budget (e.g. top *N* per category) is an implementation knob.

---

## 4. Outputs

The Planning Agent produces **one recommendation report per analysis**:

- an **overall assessment** — a short narrative on repository health, and
- a prioritized list of **recommendations**.

Each recommendation:

| Field | Type | Meaning |
| --- | --- | --- |
| `priority` | enum (`CRITICAL`/`HIGH`/`MEDIUM`/`LOW`) | Fix order. May reuse `Severity` or a dedicated `RecommendationPriority` (see §11). |
| `title` | string | Short, action-oriented (e.g. "Add tests around payment logic"). |
| `description` | string | What to do, concretely. |
| `reason` | string | Why it matters — grounded in the findings/evidence. |
| `impact` | string | Expected benefit if addressed (risk reduction, maintainability, etc.). |
| `affectedCategories` | list of `AgentType` | Which analysis areas this touches (e.g. `[SECURITY, TESTING]`). |

Report-level:

| Field | Type | Meaning |
| --- | --- | --- |
| `overallAssessment` | string | Executive summary of the repository's quality/health. |
| `recommendations` | list | Ordered by `priority`. |

These map to a new persisted entity (`Recommendation`) associated with the
`Analysis` (§9, §11).

---

## 5. Prompt Design

> Strategy only — no production prompt strings or code here.

### AI role (System Prompt)

The system prompt establishes the model as a **senior software engineer / tech
lead reviewing a static-analysis report**. Its responsibilities:

- Interpret already-collected findings; **do not invent** issues not supported by
  the provided evidence.
- Prioritize by real engineering risk (security > correctness/complexity >
  maintainability/duplication > style), while weighing severity counts.
- Be concrete and actionable; avoid generic advice.
- Produce a **small, high-value** set (e.g. 3–7 recommendations), not one per
  finding.
- Respond **only** with JSON conforming to the schema in §6 — no prose,
  markdown, or commentary outside the JSON.

### Context provided (User Prompt)

Assembled by the future `PromptBuilder` from the inputs in §3:

1. Repository metadata (name, language, build tool, file/test counts).
2. Analysis status (`COMPLETED`/`PARTIAL`) and which agents failed
   (`agentExecutions`) so the model can caveat incomplete evidence.
3. Aggregated summary: `totalFindings`, severity counts, `findingsByCategory`,
   and the testing maturity score.
4. A **bounded, severity-ranked sample** of findings per category (title,
   severity, filePath, short description).

### Expected behavior & constraints

- Ground every recommendation in the supplied evidence; cite categories via
  `affectedCategories`.
- Deduplicate/merge related findings into a single recommendation where sensible
  (e.g. many duplication findings → one "reduce duplication in module X").
- Respect the recommendation count bound.
- Deterministic-ish output: low temperature recommended (an implementation knob).

### Formatting requirements

- Output **must** be a single JSON object matching §6.
- Use the enum vocabularies exactly (`priority` ∈ {CRITICAL, HIGH, MEDIUM, LOW};
  `affectedCategories` ⊆ AgentType names).
- Request JSON-object response mode from the API where available to reduce
  malformed output.

---

## 6. Response Format

The model must return a single JSON object in this shape (illustrative):

```json
{
  "overallAssessment": "The repository has solid structure but weak test coverage and two high-severity security issues that should be addressed before the next release.",
  "recommendations": [
    {
      "priority": "CRITICAL",
      "title": "Remediate SQL injection in the reporting DAO",
      "description": "Replace string-concatenated queries in ReportDao with parameterized statements.",
      "reason": "Security analysis flagged a HIGH-severity SQL injection sink reachable from user input.",
      "impact": "Eliminates a critical data-breach vector.",
      "affectedCategories": ["SECURITY"]
    },
    {
      "priority": "HIGH",
      "title": "Add tests around the payment module",
      "description": "Introduce unit and integration tests for PaymentService and its collaborators.",
      "reason": "Testing analysis found no assertions in several payment tests and a low maturity score, while this module is also highly complex.",
      "impact": "Reduces regression risk in business-critical code.",
      "affectedCategories": ["TESTING", "COMPLEXITY"]
    }
  ]
}
```

Schema (informal):

```
Report:
  overallAssessment : string (required)
  recommendations   : Recommendation[] (required, may be empty)

Recommendation:
  priority           : "CRITICAL" | "HIGH" | "MEDIUM" | "LOW"   (required)
  title              : string   (required)
  description        : string   (required)
  reason             : string   (required)
  impact             : string   (required)
  affectedCategories : string[] (subset of AgentType names; required, may be empty)
```

Parsing/validation is **not** implemented in this issue (Issue #25).

---

## 7. Error Handling Strategy

The Planning Agent is **best-effort and non-fatal**: a failure to generate
recommendations must never corrupt or roll back the already-persisted analysis.
This mirrors the pipeline's existing failure-isolation philosophy (see the
`runAgent` try/catch in the orchestrator).

| Failure mode | Intended handling (Issue #25) |
| --- | --- |
| OpenAI API error (5xx/network) | Retry with capped exponential backoff (bounded attempts); if still failing, persist no recommendations and surface a clear "recommendations unavailable" state. |
| Timeout | Configurable request timeout (`codesentinel.planning.timeout-*`); treat as a retryable failure. |
| Rate limiting (429) | Backoff + retry within the attempt budget; then give up gracefully. |
| Malformed / non-JSON response | Validate against §6; on failure, optionally one re-ask, then fail gracefully (log, no partial persistence). |
| Empty `recommendations` | Valid outcome — persist the `overallAssessment` with an empty list; the UI shows "no recommendations". |
| Missing API key / config | Fail fast with a clear configuration error; do not call the API. |
| Incomplete evidence (`Analysis.status = PARTIAL`) | Still generate, but the prompt notes which agents failed so the assessment is caveated. |

A dedicated `PlanningAgentException` (unchecked) plus a `GlobalExceptionHandler`
entry should follow the existing per-agent exception pattern
(`SecurityAnalysisException`, etc.).

---

## 8. Recommendation Generation Flow

```
Analysis Completed (findings persisted under an Analysis)
        │
        ▼
Retrieve Findings + metadata + aggregated summary   (by analysisId)
        │
        ▼
Build Prompt (PromptBuilder: summary + bounded findings sample)
        │
        ▼
Call OpenAI (OpenAIClient, with timeout + retries)
        │
        ▼
Validate JSON against the §6 schema
        │
        ├── invalid / error ──► handle per §7 (no partial persistence)
        ▼
Persist Recommendations + overallAssessment  (linked to the Analysis)
        │
        ▼
Expose via REST API  ──►  Frontend
```

**Trigger options (decided in §11):**
- *On demand:* a `RecommendationController` endpoint takes an `analysisId` and
  runs planning for an already-completed analysis. (Preferred for MVP — keeps the
  analysis pipeline fast and independent of the LLM.)
- *Inline:* `UnifiedAnalysisServiceImpl` calls the Planning Agent at the end of a
  run. Simpler UX, but couples analysis latency/reliability to OpenAI.

---

## 9. Future Components (built in Issue #25 — do not create now)

| Component | Type | Responsibility |
| --- | --- | --- |
| `PlanningAgent` (+ `impl`) | interface + `@Component` | Orchestrates: load evidence → build prompt → call OpenAI → validate → return report. Note: unlike the analysis agents it consumes findings, **not** a `Path`, so its interface differs (e.g. `plan(Long analysisId)` / `plan(Analysis)`). |
| `PromptBuilder` | `@Component` | Assembles system + user prompts from findings, metadata, and the aggregated summary; enforces the token budget. |
| `OpenAIClient` | `@Component` | Thin wrapper over the OpenAI API (auth, model, timeout, retries, JSON-mode). Isolates the SDK/HTTP so it's swappable/testable. |
| `PlanningProperties` | `@ConfigurationProperties("codesentinel.planning")` | `api-key`, `model`, `timeout`, `max-tokens`, `temperature`, retry/backoff, `max-recommendations`, per-category finding sample size. Follows the existing `SecurityProperties`/`application.properties` pattern. |
| `Recommendation` | JPA `@Entity` | Persisted recommendation, `@ManyToOne` → `Analysis` (FK `analysis_id`), fields per §4. |
| `RecommendationRepository` | `JpaRepository` | e.g. `findByAnalysisId(Long)`. |
| `RecommendationService` (+ `impl`) | interface + `@Service` (`@Transactional`) | Coordinates the Planning Agent + persistence; maps entities → DTOs. |
| `RecommendationController` | `@RestController` | e.g. `POST /api/analysis/{analysisId}/recommendations` (generate), `GET /api/analysis/{analysisId}/recommendations` (fetch). |
| `RecommendationDto` / `RecommendationReportResponse` | DTOs | API response shape mirroring §6, consistent with existing `*Response` DTOs and `FindingDto`. |
| `PlanningAgentException` + handler entry | exception | Consistent error handling per §7. |
| `RecommendationPriority` (optional) | enum | If not reusing `Severity`. |

Model/enum note: `AgentType.PLANNING` already exists; `affectedCategories` reuse
the existing `AgentType` vocabulary.

---

## 10. Sequence Diagram

On-demand generation for a completed analysis (preferred MVP flow):

```
User        Frontend        Backend/API        RecommendationService     PlanningAgent      OpenAI        Database
 │              │                 │                      │                     │               │              │
 │ view results │                 │                      │                     │               │              │
 ├─────────────►│                 │                      │                     │               │              │
 │              │ POST /api/analysis/{id}/recommendations│                     │               │              │
 │              ├────────────────►│                      │                     │               │              │
 │              │                 │ generate(analysisId) │                     │               │              │
 │              │                 ├─────────────────────►│                     │               │              │
 │              │                 │                      │ load findings+meta  │               │              │
 │              │                 │                      ├────────────────────────────────────────────────►  │
 │              │                 │                      │◄──────────────────────────────────────────────── │
 │              │                 │                      │ plan(analysis)      │               │              │
 │              │                 │                      ├────────────────────►│               │              │
 │              │                 │                      │                     │ build prompt  │              │
 │              │                 │                      │                     │ call OpenAI   │              │
 │              │                 │                      │                     ├──────────────►│              │
 │              │                 │                      │                     │◄──────────────┤ JSON         │
 │              │                 │                      │                     │ validate §6   │              │
 │              │                 │                      │◄────────────────────┤ Report        │              │
 │              │                 │                      │ persist recommendations              │             │
 │              │                 │                      ├────────────────────────────────────────────────► │
 │              │                 │◄─────────────────────┤ RecommendationReportResponse         │             │
 │              │◄────────────────┤                      │                     │               │              │
 │◄─────────────┤ render          │                      │                     │               │              │
```

---

## 11. Design Decisions

| Decision | Rationale |
| --- | --- |
| **Recommendations are generated *after* analysis completes.** | The Planning Agent needs the full body of evidence to prioritize across categories. Running it mid-pipeline would give partial input. It also keeps the (fast, deterministic) analysis pipeline decoupled from the (slow, non-deterministic, external) LLM call. |
| **The agent consumes persisted findings, not source code.** | Findings are already normalized, tool-validated, and compact. Re-reading source would duplicate the analysis agents' work, blow the token budget, and reduce signal. It also lets planning run on an existing `analysisId` without a clone. |
| **On-demand trigger (by `analysisId`) preferred for MVP over inline.** | Keeps `POST /api/analysis/analyze` fast and resilient (no OpenAI dependency in the core path). Recommendations become an explicit, retryable step. An inline hook remains a future option. |
| **Structured JSON responses (JSON mode + schema).** | Deterministic parsing/persistence, straightforward validation (§6), and a stable API contract. Free-text would be brittle to parse and inconsistent to display. |
| **Recommendations are persisted (new tables).** | So the UI can re-display them without re-calling OpenAI (cost/latency), they can back Analysis History (Issue #23), and generation stays idempotent-ish per analysis. |
| **Best-effort, non-fatal generation.** | Mirrors the pipeline's failure isolation: a Planning/OpenAI failure must never damage the persisted analysis. |
| **Bounded, summarized prompt input.** | Controls token cost and improves focus; a severity-ranked sample plus aggregate counts captures the signal without dumping every finding. |
| **New `Recommendation` entity rather than reusing `Finding`.** | Findings are objective facts from tools; recommendations are interpretive and carry different fields (`reason`, `impact`, `affectedCategories`, `priority`). Conflating them would muddy both models. |

---

## 12. Assumptions

The Issue #25 implementation may assume:

- An `Analysis` already exists and has completed (`status = COMPLETED` or
  `PARTIAL`); its `Finding`s are persisted and retrievable by `analysisId`.
- The Planning Agent runs against persisted data only — no repository clone or
  filesystem access is required or available.
- A valid **OpenAI API key** and model are configured via
  `codesentinel.planning.*` (following the existing properties pattern).
- Recommendation generation is **synchronous** for the MVP (request → OpenAI →
  persist → respond); async/queued generation is a future enhancement.
- A `PARTIAL` analysis may have incomplete evidence; the prompt will note which
  agents failed, and the assessment will be caveated accordingly.
- Network egress to the OpenAI API is permitted from the backend environment.
- Token/cost limits are acceptable for MVP volumes; prompt input is bounded to
  keep requests within model context and budget.

---

## Appendix — Alignment with the current codebase

- Reuses `Analysis`, `Finding`, `RepositoryEntity`, `AgentType` (incl. the
  existing `PLANNING` value), and `Severity` (`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`).
- Reads from `FindingRepository` / `AnalysisRepository` and the
  `UnifiedAnalysisResponse` summary already produced by
  `UnifiedAnalysisServiceImpl`.
- New components follow existing conventions: interface + `impl`, `@Service`
  `@Transactional`, `@ConfigurationProperties`, `*Response`/`*Dto` DTOs with
  static `from(...)` factories, `JpaRepository`, and per-feature exceptions routed
  through `GlobalExceptionHandler`.
- Endpoints live under `/api/...` consistent with the existing controllers.
```

