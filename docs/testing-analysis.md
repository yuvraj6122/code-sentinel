# Testing Analysis Agent

The Testing Analysis Agent evaluates the **quality, maturity, and
maintainability** of a repository's test suite. It answers a single question:

> How mature and effective is the testing strategy in this repository?

It deliberately does **not** count tests or compute test/source ratios — those
are already reported by the Repository Scanner. Instead it inspects *how* tests
are written and organized, records each concern as a `Finding` (reusing the
shared model and persistence pipeline), and derives an overall 0–100 **Testing
Maturity Score**.

## Overview

```
Repository
    ↓
Repository Clone Service       (clones the GitHub repository locally)
    ↓
Testing Analysis Agent         (analyze(Path repositoryPath))
    ↓
Repository Inspection          (TestFrameworkDetector + TestSourceInspector, JavaParser)
    ↓
Testing Quality Analysis       (8 graded checks + maturity scoring)
    ↓
Finding Generation             (AgentType.TESTING, shared Finding model)
    ↓
Persistence                    (FindingRepository, Analysis workflow)
    ↓
API Response                   (TestingAnalysisResponse)
```

## Repository Inspection

Two collaborators feed the agent, mirroring how the Complexity Agent uses
`SourceLengthAnalyzer`:

- **`TestFrameworkDetector`** reads the build files (`pom.xml`, `build.gradle`,
  `build.gradle.kts`) and, as a fallback for repositories that inherit
  dependencies from a parent/BOM, the imports observed in test sources.
- **`TestSourceInspector`** walks the repository with **JavaParser**, parses each
  test source once, and produces a PMD/JavaParser-agnostic `TestSourceScan`:
  the test classes (with their `@Test` methods, disabled/assertion/integration
  facts) plus the package layout of `src/main/java` vs `src/test/java`.

A file is treated as a test source when it lives under `src/test/` or its name
ends with `Test`, `Tests`, `IT`, or `ITCase`.

## Implemented Checks

| # | Check | Detects | Severity |
| - | ----- | ------- | -------- |
| 1 | Testing Framework Detection | JUnit 4, JUnit 5, TestNG | **HIGH** when none found |
| 2 | Mocking Framework Detection | Mockito, EasyMock, MockK | **LOW** when none found (and tests exist) |
| 3 | Integration Test Detection | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, `@JdbcTest`, Testcontainers, `*IT` / `*IntegrationTest` naming | **MEDIUM** when tests exist but none found |
| 4 | Disabled / Ignored Tests | `@Disabled`, `@Ignore` | graded by count (see below) |
| 5 | Empty Test Classes | test-named classes with no `@Test` methods | **LOW** per class |
| 6 | Tests Without Assertions | `@Test` methods with no `assert*` / `Assertions.*` / `Assert.*` / `fail` / Mockito `verify` | **MEDIUM** per method |
| 7 | Test Naming Quality | non-descriptive names (`test1`, `abc`, `check`, `sample`, …) | **LOW** per method |
| 8 | Test Organization | test packages that do not mirror `src/main/java` packages | **LOW** |

### Assertion detection

A test method is considered to *verify* something if it invokes any call named
`assert*` or `fail`, a call qualified by a known assertion type (e.g.
`Assertions.*`, `Assert.*`, `MatcherAssert.*`), or a Mockito `verify(...)`.
Disabled methods are excluded from the assertion and naming checks (they don't
run).

### Disabled-test severity mapping

The disabled/ignored test count is graded via configurable thresholds:

| Disabled tests | Severity |
| -------------- | -------- |
| 0              | Ignored (no finding) |
| 1 – 5          | LOW      |
| 6 – 10         | MEDIUM   |
| 11+            | HIGH     |

A class annotated `@Disabled`/`@Ignore` counts all of its test methods as
disabled.

## Severity Mapping (summary)

- **HIGH** — no testing framework detected; a large number of disabled tests.
- **MEDIUM** — no integration tests; tests without assertions.
- **LOW** — poor naming; poor organization; empty test classes; no mocking
  framework.

## Testing Maturity Score (0–100)

The maturity score summarizes the checks into a single number. It starts at
**100** and deducts configurable penalties. Per-item penalties are capped so a
single category cannot dominate:

| Condition | Deduction (default) |
| --------- | ------------------- |
| No tests present | score is **0** (`No Tests`) |
| No testing framework | −40 |
| No mocking framework | −8 |
| No integration tests | −15 |
| Disabled tests | −3 each, capped at −20 |
| Tests without assertions | −4 each, capped at −20 |
| Empty test classes | −3 each, capped at −12 |
| Poor test names | −1 each, capped at −10 |
| Poor organization | −8 |

The score is clamped to `[0, 100]` and mapped to a summary label: `Excellent`
(≥80), `Good` (≥60), `Moderate` (≥40), `Weak` (≥20), `Poor` (<20), or
`No Tests` when the repository has no test methods.

All thresholds and penalties are tunable in `application.properties` and are
never hard-coded in the agent:

```properties
codesentinel.testing.disabled-tests.low-threshold=1
codesentinel.testing.disabled-tests.medium-threshold=6
codesentinel.testing.disabled-tests.high-threshold=11

codesentinel.testing.maturity.no-framework-penalty=40
codesentinel.testing.maturity.no-mocking-penalty=8
codesentinel.testing.maturity.no-integration-penalty=15
codesentinel.testing.maturity.disabled-test-penalty-per-test=3
codesentinel.testing.maturity.disabled-test-penalty-cap=20
codesentinel.testing.maturity.no-assertion-penalty-per-test=4
codesentinel.testing.maturity.no-assertion-penalty-cap=20
codesentinel.testing.maturity.empty-class-penalty-per-class=3
codesentinel.testing.maturity.empty-class-penalty-cap=12
codesentinel.testing.maturity.poor-naming-penalty-per-method=1
codesentinel.testing.maturity.poor-naming-penalty-cap=10
codesentinel.testing.maturity.poor-organization-penalty=8
```

## Finding Generation

Each concern becomes a `Finding` with:

- `agentType = TESTING`
- `severity` = the graded value (LOW / MEDIUM / HIGH)
- `title`, e.g. `Disabled Tests Detected`
- `description`, e.g. `8 disabled tests were found.`
- `filePath` = the offending test class (when applicable; repository-wide checks
  such as framework/integration detection carry no file)

Findings reuse the existing `Finding` / `Analysis` models, `FindingRepository`,
and analysis workflow — no separate testing storage model is introduced.

## API

`POST /api/testing/analyze`

Request (reuses `CloneRepositoryRequest`):

```json
{ "githubUrl": "https://github.com/owner/repo" }
```

Response (`TestingAnalysisResponse`) — the shared finding-summary shape plus the
testing-maturity fields:

```json
{
  "analysisId": 42,
  "totalFindings": 3,
  "highSeverity": 0,
  "mediumSeverity": 1,
  "lowSeverity": 2,
  "testingFrameworks": ["JUnit 5"],
  "mockingFrameworks": [],
  "hasTests": true,
  "hasIntegrationTests": false,
  "testClassCount": 12,
  "testMethodCount": 47,
  "disabledTestCount": 2,
  "maturityScore": 66,
  "maturitySummary": "Good",
  "findings": [
    {
      "agentType": "TESTING",
      "severity": "MEDIUM",
      "title": "No Integration Tests Detected",
      "description": "Only unit tests were found; no integration testing patterns (e.g. @SpringBootTest, @DataJpaTest, Testcontainers) were detected.",
      "filePath": null
    }
  ]
}
```

Testing findings appear alongside Complexity, Duplicate Code, and (future)
Security findings through the same pipeline.

## Frontend

The dashboard renders a **Testing Analysis** section (`TestingSection`) using
the shared `DashboardSection` and `MetricCard` components. It displays:

- the Testing Maturity Score with a colored progress bar and summary label,
- detected testing and mocking frameworks (as chips),
- integration-test status, test-class/method counts, and disabled-test count,
- severity totals, and
- a table of testing findings (severity, issue, description, file).

When no testing data is available the section falls back to the existing
"Coming Soon" placeholder.

## Testing

- `TestingPropertiesTest` — the disabled-test count → severity mapping.
- `TestFrameworkDetectorTest` — framework/mocking detection from Gradle and
  Maven build files and the test-import fallback (including the "none" case).
- `TestSourceInspectorTest` — JavaParser extraction of test methods, assertion
  and disabled facts, integration detection, and package collection.
- `TestingAnalysisAgentImplTest` — end-to-end over temporary repositories:
  the no-framework HIGH finding and 0 maturity, the full spread of quality
  findings, the disabled-count HIGH mapping, and a healthy suite scoring 100.
