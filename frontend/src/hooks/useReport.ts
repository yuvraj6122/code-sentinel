import { useCallback, useState } from 'react';
import { getReport } from '../api/client';
import { ApiError } from '../types/api';

type ReportState = 'idle' | 'loading' | 'success' | 'error';

interface UseReportReturn {
  state: ReportState;
  markdown: string | null;
  error: string | null;
  load: (analysisId: number) => Promise<void>;
  reset: () => void;
}

/**
 * Fetches the consolidated engineering report (Markdown) for an already-completed
 * analysis. It reads existing data only — no repository analysis or recommendation
 * generation is triggered.
 */
export function useReport(): UseReportReturn {
  const [state, setState] = useState<ReportState>('idle');
  const [markdown, setMarkdown] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (analysisId: number) => {
    setState('loading');
    setError(null);
    setMarkdown(null);

    try {
      const result = await getReport(analysisId);
      setMarkdown(result);
      setState('success');
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : 'Could not generate the report. Please try again.';
      setError(message);
      setState('error');
    }
  }, []);

  const reset = useCallback(() => {
    setState('idle');
    setMarkdown(null);
    setError(null);
  }, []);

  return { state, markdown, error, load, reset };
}
