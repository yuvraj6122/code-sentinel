import { useCallback, useState } from 'react';
import { generateRecommendations } from '../api/client';
import { ApiError, type RecommendationReport } from '../types/api';

type RecommendationsState = 'idle' | 'loading' | 'success' | 'error';

interface UseRecommendationsReturn {
  state: RecommendationsState;
  report: RecommendationReport | null;
  error: string | null;
  generate: (analysisId: number) => Promise<void>;
  reset: () => void;
}

/**
 * Drives the AI Planning Agent for an already-completed analysis. It operates on
 * the existing {@code analysisId} produced by the unified analysis and never
 * triggers a second repository analysis.
 */
export function useRecommendations(): UseRecommendationsReturn {
  const [state, setState] = useState<RecommendationsState>('idle');
  const [report, setReport] = useState<RecommendationReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  const generate = useCallback(async (analysisId: number) => {
    setState('loading');
    setError(null);
    setReport(null);

    try {
      const result = await generateRecommendations(analysisId);
      setReport(result);
      setState('success');
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : 'Could not generate recommendations. Please try again.';
      setError(message);
      setState('error');
    }
  }, []);

  const reset = useCallback(() => {
    setState('idle');
    setReport(null);
    setError(null);
  }, []);

  return { state, report, error, generate, reset };
}
