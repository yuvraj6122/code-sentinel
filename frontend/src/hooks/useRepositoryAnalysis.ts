import { useCallback, useState } from 'react';
import { analyzeRepository } from '../api/client';
import { ApiError } from '../types/api';
import type { AnalysisResult, AnalysisState } from '../types/repository';

interface UseRepositoryAnalysisReturn {
  state: AnalysisState;
  result: AnalysisResult | null;
  error: string | null;
  analyze: (githubUrl: string) => Promise<void>;
  reset: () => void;
}

export function useRepositoryAnalysis(): UseRepositoryAnalysisReturn {
  const [state, setState] = useState<AnalysisState>('idle');
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const analyze = useCallback(async (githubUrl: string) => {
    setState('loading');
    setError(null);
    setResult(null);

    try {
      const metadata = await analyzeRepository({ githubUrl });
      setResult({ metadata, analyzedUrl: githubUrl });
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
