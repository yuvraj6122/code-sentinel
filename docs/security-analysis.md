# Security Analysis Agent

The Security Analysis Agent analyzes Java repositories for security
vulnerabilities and security-relevant code quality issues. It answers:

> What security risks exist in this repository?

Rather than hand-rolling detectors, it integrates and operationalizes two proven
static-analysis tools — **SpotBugs** and the **FindSecBugs** plugin — and maps
their results into CodeSentinel's shared `Finding` model, persisted and exposed
through the same pipeline used by the other agents.

## Overview

```
Repository
    ↓
Repository Clone Service        (clones the GitHub repository locally)
    ↓
Security Analysis Agent         (analyze(Path repositoryPath))
    ↓
Build Repository (best-effort)  (RepositoryBuilder → compiled .class dirs)
    ↓
Run SpotBugs + FindSecBugs      (SpotBugsRunner, embedded, XML report)
    ↓
Parse Report                    (SpotBugsReportParser → SpotBugsBug)
    ↓
Generate Findings               (SecurityFindingFactory, AgentType.SECURITY)
    ↓
Persist Findings                (FindingRepository, Analysis workflow)
    ↓
API Response                    (SecurityAnalysisResponse)
```

## Why a build step?

SpotBugs analyzes **bytecode**, not source, so the repository must be compiled
first. `RepositoryBuilder` detects the build tool and runs a time-boxed compile:

- Maven (`pom.xml`): `mvn -B -q -DskipTests compile` (prefers `./mvnw` if present)
- Gradle (`build.gradle`/`build.gradle.kts`): `gradle compileJava -x test`
  (prefers `./gradlew` if present)

It then locates the produced class output directories (`target/classes`,
`build/classes/java/main`, including multi-module layouts). Building arbitrary
third-party repositories is inherently unreliable, so **every failure mode**
(no build tool, non-zero exit, timeout, missing binary) is handled gracefully:
the agent analyzes whatever bytecode exists and, if none is found, returns **no
findings** instead of failing. This keeps the overall analysis flow working even
when one agent can't complete.

The build step is configurable and can be disabled to analyze only pre-existing
bytecode.

## SpotBugs + FindSecBugs Integration

`SpotBugsRunner` drives SpotBugs embedded through its Java API
(`FindBugs2` + `Project` + `XMLBugReporter`), the same "run the tool, emit an
XML report, parse it" approach the Complexity and Duplicate Code agents use with
PMD. The **FindSecBugs** plugin is on the classpath and is auto-discovered by
SpotBugs' `DetectorFactoryCollection`, so its security detectors run alongside
SpotBugs' own. Source roots (`src/main/java`) are registered so findings resolve
to source line numbers, and a priority threshold limits noise.

## Supported Finding Categories

FindSecBugs and SpotBugs provide the detections; the agent surfaces bugs whose
category is `SECURITY` or `MALICIOUS_CODE` (all FindSecBugs detections), plus a
configurable set of security-relevant SpotBugs bug-type prefixes. Covered
categories include:

| # | Category | Example detections | Typical severity |
| - | -------- | ------------------ | ---------------- |
| 1 | Hardcoded credentials | hardcoded passwords / API keys / secrets | HIGH |
| 2 | SQL injection | dynamic query construction from user input | HIGH |
| 3 | Path traversal | unsafe file access using external input | HIGH |
| 4 | Command injection | `Runtime.exec()` / `ProcessBuilder` misuse | HIGH |
| 5 | Weak cryptography | MD5, SHA-1 | HIGH |
| 6 | Weak randomness | `java.util.Random` for security-sensitive use | MEDIUM |
| 7 | Null dereferences | possible null dereference (`NP_*`) | LOW / MEDIUM |
| 8 | Resource leaks | unclosed streams / DB resources (`OS_OPEN_STREAM`, `OBL_`, `ODR_`) | MEDIUM |
| 9 | Insecure deserialization | untrusted object deserialization | HIGH |

The exact severity of each finding is derived from the tool's reported priority
(see below), which for these detectors aligns with the guidance above.

## Report Parsing

`SpotBugsReportParser` DOM-parses the SpotBugs XML report (mirroring
`PmdReportParser`) into raw `SpotBugsBug` records, extracting:

- **type** (e.g. `PREDICTABLE_RANDOM`, `HARD_CODE_PASSWORD`)
- **category** (e.g. `SECURITY`)
- **priority** and **rank**
- **description** (long message, falling back to short message / humanized type)
- **file path** (the primary `SourceLine`'s `sourcepath`)
- **line number** (the primary `SourceLine`'s `start`)

## Severity Mapping

All SpotBugs/FindSecBugs priority → `Severity` conversion lives in one place,
`SecuritySeverityMapper`, rather than being scattered through the parsing/finding
code. SpotBugs encodes priority as a number where lower is more severe:

| SpotBugs priority | CodeSentinel severity |
| ----------------- | --------------------- |
| High (1)          | HIGH                  |
| Medium (2)        | MEDIUM                |
| Low (3+)          | LOW                   |

## Finding Generation & Persistence

`SecurityFindingFactory` converts each qualifying `SpotBugsBug` into a `Finding`:

- `agentType = SECURITY`
- `severity` from `SecuritySeverityMapper`
- `title` (SpotBugs short message)
- `description` (SpotBugs long message)
- `filePath` and `lineNumber` (when resolvable)
- `analysis` association (set by the service)

Findings reuse the existing `Finding` / `Analysis` models, `FindingRepository`,
and analysis workflow. The shared `Finding` gained a nullable `lineNumber`
column so agents that can resolve a line (SpotBugs) may record it; other agents
leave it null.

## API

`POST /api/security/analyze`

Request (reuses `CloneRepositoryRequest`):

```json
{ "githubUrl": "https://github.com/owner/repo" }
```

Response (`SecurityAnalysisResponse`, same shape as the Complexity/Duplicate
responses):

```json
{
  "analysisId": 21,
  "totalFindings": 3,
  "highSeverity": 2,
  "mediumSeverity": 1,
  "lowSeverity": 0,
  "findings": [
    {
      "agentType": "SECURITY",
      "severity": "HIGH",
      "title": "Hard coded password",
      "description": "Hard coded password found in DbConfig.java",
      "filePath": "com/example/DbConfig.java",
      "lineNumber": 18
    }
  ]
}
```

Security findings appear alongside Complexity, Duplicate Code, and Testing
findings. As with the other agents, the frontend calls this endpoint in parallel
and degrades gracefully if it fails.

## Frontend

The dashboard renders a **Security Analysis** section (`SecuritySection`) using
the shared `DashboardSection`, `MetricCard`, and `FindingsAccordion` components.
It shows total findings and high/medium/low counts, then the findings grouped by
severity in collapsible bars; each finding displays its title, severity, file
path (with line number), and description. When no security data is available the
section falls back to the existing "Coming Soon" placeholder.

## Testing

- `SpotBugsReportParserTest` — parses sample SpotBugs XML into bugs (type,
  category, priority, messages, primary source line / line number; humanized
  fallback when messages are absent).
- `SecuritySeverityMapperTest` — the priority → severity mapping.
- `SecurityFindingFactoryTest` — security-finding generation: category and
  bug-type-prefix filtering, priority-threshold dropping, and field mapping
  (agent type, severity, file, line).
- `SecurityAnalysisResponseTest` — API response generation and severity
  counting (persistence uses the same `Finding` model the response is built
  from).
- `SecurityAnalysisAgentImplTest` — the graceful path: a source-only repository
  with no bytecode yields no findings and never invokes SpotBugs.
