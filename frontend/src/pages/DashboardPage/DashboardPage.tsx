import { ErrorBanner } from '../../components/ErrorBanner/ErrorBanner';
import { LoadingSpinner } from '../../components/LoadingSpinner/LoadingSpinner';
import { RepositoryForm } from '../../components/RepositoryForm/RepositoryForm';
import { ResultsDashboard } from '../../components/ResultsDashboard/ResultsDashboard';
import { useRepositoryAnalysis } from '../../hooks/useRepositoryAnalysis';
import styles from './DashboardPage.module.css';

export function DashboardPage() {
  const { state, result, error, analyze, reset } = useRepositoryAnalysis();

  const isLoading = state === 'loading';
  const showResults = state === 'success' && result !== null;

  return (
    <div className={styles.page}>
      <section className={styles.hero} aria-labelledby="hero-title">
        <span className={styles.heroBadge}>
          <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor">
            <path d="M11.3 1.046a1 1 0 011.414 0l2.829 2.829a1 1 0 010 1.414l-8.486 8.485a1 1 0 01-.707.293H4a1 1 0 01-1-1v-2.343a1 1 0 01.293-.707l8.485-8.485zM14.828 4.172L13.414 2.757 5.343 10.828l1.414 1.415 8.071-8.071z" />
          </svg>
          Multi-Agent Analysis
        </span>

        <h1 className={styles.heroTitle} id="hero-title">
          CodeSentinel
        </h1>
        <p className={styles.heroSubtitle}>AI-Powered Code Quality Analysis</p>
        <p className={styles.heroDescription}>
          Analyze GitHub repositories for code complexity, security issues, testing
          quality, duplicate code, and technical debt — powered by intelligent
          agents.
        </p>
      </section>

      <section className={styles.analysisSection} aria-labelledby="analyze-label">
        <h2 className={styles.sectionLabel} id="analyze-label">
          Repository Analysis
        </h2>
        <RepositoryForm onSubmit={analyze} isLoading={isLoading} />
      </section>

      {error && (
        <div className={styles.errorWrapper}>
          <ErrorBanner message={error} onDismiss={reset} />
        </div>
      )}

      {isLoading && (
        <div className={styles.loadingState} role="status">
          <LoadingSpinner size="lg" />
          <p className={styles.loadingText}>
            Cloning and scanning repository…
          </p>
        </div>
      )}

      {showResults && (
        <div className={styles.resultsWrapper}>
          <ResultsDashboard
            metadata={result.metadata}
            duplication={result.duplication}
            analyzedUrl={result.analyzedUrl}
          />
        </div>
      )}

      {!showResults && !isLoading && (
        <section className={styles.features} aria-label="Platform capabilities">
          <div className={styles.feature}>
            <svg className={styles.featureIcon} viewBox="0 0 20 20" fill="currentColor">
              <path
                fillRule="evenodd"
                d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
            <h3 className={styles.featureTitle}>Complexity Analysis</h3>
            <p className={styles.featureDesc}>
              Detect overly complex methods and architectural hotspots
            </p>
          </div>

          <div className={styles.feature}>
            <svg className={styles.featureIcon} viewBox="0 0 20 20" fill="currentColor">
              <path
                fillRule="evenodd"
                d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <h3 className={styles.featureTitle}>Security Scanning</h3>
            <p className={styles.featureDesc}>
              Identify vulnerabilities and insecure coding patterns
            </p>
          </div>

          <div className={styles.feature}>
            <svg className={styles.featureIcon} viewBox="0 0 20 20" fill="currentColor">
              <path d="M13 7H7v6h6V7z" />
              <path
                fillRule="evenodd"
                d="M7 2a1 1 0 012 0v1h2V2a1 1 0 112 0v1h2a2 2 0 012 2v2h1a1 1 0 110 2h-1v2h1a1 1 0 110 2h-1v2a2 2 0 01-2 2h-2v1a1 1 0 11-2 0v-1H9v1a1 1 0 11-2 0v-1H5a2 2 0 01-2-2v-2H2a1 1 0 110-2h1V9H2a1 1 0 010-2h1V5a2 2 0 012-2h2V2zM5 5h10v10H5V5z"
                clipRule="evenodd"
              />
            </svg>
            <h3 className={styles.featureTitle}>AI Recommendations</h3>
            <p className={styles.featureDesc}>
              Actionable suggestions to reduce debt and improve quality
            </p>
          </div>
        </section>
      )}
    </div>
  );
}
