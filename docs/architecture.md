# CodeSentinel Basic Architecture

## Agents

1. Repository Scanner Agent
2. Complexity Analysis Agent
3. Security Analysis Agent
4. Testing Analysis Agent
5. Duplicate Code Analysis Agent
6. Planning Agent

## Workflow

GitHub Repository → Repository Scanner → Analysis Agents → Planning Agent → Final Report

The [Unified Analysis Orchestrator](unified-analysis.md) drives this workflow in
a single request: clone → scan → run every analysis agent (resiliently) →
aggregate and persist findings under one analysis record → return a
repository-level summary. It coordinates the existing agents rather than
duplicating them; the per-agent endpoints remain available for single-agent runs.

## Agent Documentation

- [Unified Analysis Orchestrator](unified-analysis.md) — end-to-end pipeline, agent execution flow, failure handling, and analysis lifecycle.
- [Duplicate Code Analysis Agent](duplicate-code-analysis.md) — PMD CPD integration and severity thresholds.
- [Testing Analysis Agent](testing-analysis.md) — test-suite quality/maturity checks, severity mappings, and the 0–100 maturity score.
- [Security Analysis Agent](security-analysis.md) — SpotBugs + FindSecBugs integration, priority→severity mapping, and supported finding categories.
- [Planning Agent](planning-agent-design.md) — **design only:** how the AI Planning Agent will interpret findings and generate prioritized recommendations.