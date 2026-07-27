import { useCallback, useState } from 'react';
import { runUnifiedAnalysis } from '../api/client';
import {
  ApiError,
  type Finding,
  type Severity,
  type TestingAnalysis,
  type UnifiedAnalysis,
} from '../types/api';
import type { AnalysisResult, AnalysisState } from '../types/repository';

interface UseRepositoryAnalysisReturn {
  state: AnalysisState;
  result: AnalysisResult | null;
  error: string | null;
  analyze: (githubUrl: string) => Promise<void>;
  reset: () => void;
}

function findingsFor(findings: Finding[], agentType: string): Finding[] {
  return findings.filter((finding) => finding.agentType === agentType);
}

function severityCount(findings: Finding[], severity: Severity): number {
  return findings.filter((finding) => finding.severity === severity).length;
}

/** Builds the shared finding-summary shape (used by every non-testing section). */
function section(analysisId: number, findings: Finding[], agentType: string) {
  const items = findingsFor(findings, agentType);
  return {
    analysisId,
    totalFindings: items.length,
    highSeverity: severityCount(items, 'HIGH'),
    mediumSeverity: severityCount(items, 'MEDIUM'),
    lowSeverity: severityCount(items, 'LOW'),
    findings: items,
  };
}

function toTesting(analysis: UnifiedAnalysis): TestingAnalysis | null {
  if (!analysis.testingMetrics) {
    return null;
  }
  const items = findingsFor(analysis.findings, 'TESTING');
  return {
    analysisId: analysis.analysisId,
    totalFindings: items.length,
    highSeverity: severityCount(items, 'HIGH'),
    mediumSeverity: severityCount(items, 'MEDIUM'),
    lowSeverity: severityCount(items, 'LOW'),
    ...analysis.testingMetrics,
    findings: items,
  };
}

/**
 * Runs a single Unified Analysis Orchestrator request and derives every
 * dashboard section from its result. All agents run once, under one analysis
 * record, so the Planning Agent can later operate on the same {@code analysisId}
 * without triggering a second repository analysis.
 */
export function useRepositoryAnalysis(): UseRepositoryAnalysisReturn {
  const [state, setState] = useState<AnalysisState>('idle');
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const analyze = useCallback(async (githubUrl: string) => {
    setState('loading');
    setError(null);
    setResult(null);

    try {
      const analysis = await runUnifiedAnalysis({ githubUrl });
      const { analysisId, findings } = analysis;

      setResult({
        analysisId,
        metadata: analysis.metadata,
        complexity: section(analysisId, findings, 'COMPLEXITY'),
        duplication: section(analysisId, findings, 'DUPLICATE_CODE'),
        testing: toTesting(analysis),
        security: section(analysisId, findings, 'SECURITY'),
        analyzedUrl: githubUrl,
      });
      setState('success');
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : 'An unexpected error occurred. Please try again.';
      setError(message);
      setState('error');
    }
  }, []);

  const reset = useCallback(() => {
    setState('idle');
    setResult(null);
    setError(null);
  }, []);

  return { state, result, error, analyze, reset };
}
