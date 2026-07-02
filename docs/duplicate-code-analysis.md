# Duplicate Code Analysis Agent

The Duplicate Code Analysis Agent detects copy/pasted code across a repository
and records each significant duplication as a `Finding`, persisted and exposed
through the same analysis pipeline used by the other agents.

## Overview

```
Repository
    ↓
Repository Clone Service      (clones the GitHub repository locally)
    ↓
Duplicate Code Analysis Agent (analyze(Path repositoryPath))
    ↓
Run PMD CPD                   (Copy/Paste Detector, Java)
    ↓
Parse CPD XML Report          (CpdReportParser → CpdDuplication)
    ↓
Convert to Findings           (grade by duplicated line count)
    ↓
Store Findings                (FindingRepository, AgentType.DUPLICATE_CODE)
```

## PMD CPD Integration

The agent uses **PMD CPD (Copy/Paste Detector)** — the same PMD toolchain the
Complexity Analysis Agent relies on — rather than a custom duplication
algorithm. Java is supported initially.

Implementation notes:

- `DuplicateCodeAnalysisAgentImpl` configures `CPDConfiguration`, restricts
  analysis to the Java language, and points CPD at the cloned repository path.
- Paths are relativized against the repository root so findings reference
  repository-relative file paths.
- CPD results are rendered to a temporary **XML** report using PMD's
  `XMLRenderer` — mirroring the Complexity Agent, which also emits and parses an
  XML report.
- `CpdReportParser` parses the XML into `CpdDuplication` records (each with a
  duplicated line count and the set of `CpdMark` file occurrences), keeping the
  raw PMD representation separate from the internal `Finding` model so PMD can be
  swapped out later.

### Detection knob: minimum tokens

CPD detects duplication in terms of **tokens**, not lines. The
`codesentinel.duplication.minimum-tokens` property (default `50`) sets the
smallest block CPD will report. It is intentionally lower than the line
thresholds so shorter blocks surface and are then graded by line count.

## Severity Thresholds

Each duplication is graded by its **duplicated line count**:

| Duplicated lines | Severity        |
| ---------------- | --------------- |
| < 20             | Ignored (no finding) |
| 20 – 49          | LOW             |
| 50 – 99          | MEDIUM          |
| 100+             | HIGH            |

Findings are only created for LOW / MEDIUM / HIGH. Thresholds are configured in
`application.properties` and are fully tunable:

```properties
codesentinel.duplication.minimum-tokens=50
codesentinel.duplication.lines.low-threshold=20
codesentinel.duplication.lines.medium-threshold=50
codesentinel.duplication.lines.high-threshold=100
```

## Finding Generation

Each reported duplication becomes a `Finding` with:

- `agentType = DUPLICATE_CODE`
- `severity` = graded value (LOW / MEDIUM / HIGH)
- `title = "Duplicate Code Detected"`
- `description`, e.g. `85 duplicated lines detected between UserService.java and AdminService.java`
- `filePath` = the primary (first) file involved in the duplication

Findings reuse the existing `Finding` / `Analysis` models, `FindingRepository`,
and analysis workflow — no separate duplication storage model is introduced.

## API

`POST /api/duplication/analyze`

Request (reuses `CloneRepositoryRequest`):

```json
{ "githubUrl": "https://github.com/owner/repo" }
```

Response (`DuplicateCodeAnalysisResponse`, same shape as the Complexity
response):

```json
{
  "analysisId": 12,
  "totalFindings": 3,
  "highSeverity": 1,
  "mediumSeverity": 1,
  "lowSeverity": 1,
  "findings": [
    {
      "agentType": "DUPLICATE_CODE",
      "severity": "HIGH",
      "title": "Duplicate Code Detected",
      "description": "120 duplicated lines detected between UserService.java and AdminService.java",
      "filePath": "src/main/java/UserService.java"
    }
  ]
}
```

## Frontend

The analysis dashboard renders a **Duplicate Code** section
(`DuplicateCodeSection`) using the shared `DashboardSection` and `MetricCard`
components. It shows total findings, high-severity count, medium/low counts, and
a table of duplicate findings (severity badge, description, file). When no
duplication data is available the section falls back to the existing
"Coming Soon" placeholder.

## Testing

- `CpdReportParserTest` — parses sample CPD XML into duplications and marks.
- `DuplicationPropertiesTest` — verifies the line-count → severity mapping
  (including the < 20 line ignore boundary).
- `DuplicateCodeAnalysisAgentImplTest` — runs real PMD CPD against a temporary
  repository containing a duplicated block and asserts a `DUPLICATE_CODE`
  finding is generated (and that non-duplicated code yields none).
