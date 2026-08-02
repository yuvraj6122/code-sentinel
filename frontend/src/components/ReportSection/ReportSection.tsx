import { useState } from 'react';
import { useReport } from '../../hooks/useReport';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { ErrorBanner } from '../ErrorBanner/ErrorBanner';
import { LoadingSpinner } from '../LoadingSpinner/LoadingSpinner';
import styles from './ReportSection.module.css';

interface ReportSectionProps {
  analysisId: number;
  repositoryName: string;
}

function fileName(repositoryName: string, analysisId: number): string {
  const safe = repositoryName.replace(/[^a-zA-Z0-9-_]/g, '-') || 'repository';
  return `${safe}-analysis-${analysisId}-report.md`;
}

export function ReportSection({ analysisId, repositoryName }: ReportSectionProps) {
  const { state, markdown, error, load, reset } = useReport();
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    if (!markdown) return;
    try {
      await navigator.clipboard.writeText(markdown);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  const handleDownload = () => {
    if (!markdown) return;
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName(repositoryName, analysisId);
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  };

  return (
    <DashboardSection
      title="Engineering Report"
      subtitle="A consolidated Markdown report of every finding and AI recommendation for this analysis"
    >
      {state === 'idle' && (
        <div className={styles.cta}>
          <p className={styles.ctaText}>
            Generate a complete engineering report that consolidates repository
            metadata, all analysis findings, and AI recommendations into a single
            Markdown document. It reads the analysis already completed above — no
            re-analysis required.
          </p>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => load(analysisId)}
          >
            Generate Report
          </button>
        </div>
      )}

      {state === 'loading' && (
        <div className={styles.loading} role="status">
          <LoadingSpinner size="lg" />
          <p className={styles.loadingText}>Assembling the engineering report…</p>
        </div>
      )}

      {state === 'error' && error && (
        <div className={styles.errorWrapper}>
          <ErrorBanner
            title="Could not generate the report"
            message={error}
            onDismiss={reset}
          />
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => load(analysisId)}
          >
            Try Again
          </button>
        </div>
      )}

      {state === 'success' && markdown && (
        <div className={styles.results}>
          <div className={styles.toolbar}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={handleCopy}
            >
              {copied ? 'Copied!' : 'Copy Markdown'}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={handleDownload}
            >
              Download .md
            </button>
            <button type="button" className={styles.ghostButton} onClick={reset}>
              Close
            </button>
          </div>
          <pre className={styles.preview}>
            <code>{markdown}</code>
          </pre>
        </div>
      )}
    </DashboardSection>
  );
}
