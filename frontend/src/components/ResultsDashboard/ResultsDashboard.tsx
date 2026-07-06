import type {
  ComplexityAnalysis,
  DuplicateCodeAnalysis,
  RepositoryMetadata,
  SecurityAnalysis,
  TestingAnalysis,
} from '../../types/api';
import { ComplexitySection } from '../ComplexitySection/ComplexitySection';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { DuplicateCodeSection } from '../DuplicateCodeSection/DuplicateCodeSection';
import { MetricCard } from '../MetricCard/MetricCard';
import { SecuritySection } from '../SecuritySection/SecuritySection';
import { TestingSection } from '../TestingSection/TestingSection';
import styles from './ResultsDashboard.module.css';

interface ResultsDashboardProps {
  metadata: RepositoryMetadata;
  duplication: DuplicateCodeAnalysis | null;
  complexity: ComplexityAnalysis | null;
  testing: TestingAnalysis | null;
  security: SecurityAnalysis | null;
  analyzedUrl: string;
}

function formatLabel(value: string): string {
  return value
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

export function ResultsDashboard({
  metadata,
  duplication,
  complexity,
  testing,
  security,
  analyzedUrl,
}: ResultsDashboardProps) {
  const testRatio =
    metadata.javaFileCount > 0
      ? Math.round((metadata.testFileCount / metadata.javaFileCount) * 100)
      : 0;

  return (
    <div className={styles.dashboard}>
      <DashboardSection title="Repository Overview">
        <div className={styles.repoHeader}>
          <div className={styles.repoIcon} aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.395-.135-.345-.72-1.395-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
            </svg>
          </div>
          <div className={styles.repoInfo}>
            <h3 className={styles.repoName}>{metadata.repositoryName}</h3>
            <p className={styles.repoUrl}>{analyzedUrl}</p>
          </div>
        </div>

        <div className={styles.grid}>
          <MetricCard
            label="Language"
            value={formatLabel(metadata.language)}
            highlight
          />
          <MetricCard label="Build Tool" value={formatLabel(metadata.buildTool)} />
          <MetricCard label="Java Files" value={metadata.javaFileCount} mono />
          <MetricCard label="Test Files" value={metadata.testFileCount} mono />
        </div>

        {metadata.javaFileCount > 0 && (
          <div className={styles.testRatio}>
            <p className={styles.ratioLabel}>Test File Ratio</p>
            <div className={styles.ratioBar}>
              <div
                className={styles.ratioFill}
                style={{ width: `${Math.min(testRatio, 100)}%` }}
              />
            </div>
            <p className={styles.ratioValue}>
              {metadata.testFileCount} of {metadata.javaFileCount} Java files are
              tests ({testRatio}%)
            </p>
          </div>
        )}
      </DashboardSection>

      {complexity ? (
        <ComplexitySection complexity={complexity} />
      ) : (
        <DashboardSection
          title="Complexity Analysis"
          subtitle="Cyclomatic complexity and method/class length checks powered by PMD"
          comingSoon
        />
      )}

      {duplication ? (
        <DuplicateCodeSection duplication={duplication} />
      ) : (
        <DashboardSection
          title="Duplicate Code"
          subtitle="Copy/paste detection powered by PMD CPD"
          comingSoon
        />
      )}

      {testing ? (
        <TestingSection testing={testing} />
      ) : (
        <DashboardSection
          title="Testing Analysis"
          subtitle="Test-suite quality and maturity checks"
          comingSoon
        />
      )}

      {security ? (
        <SecuritySection security={security} />
      ) : (
        <DashboardSection
          title="Security Analysis"
          subtitle="Vulnerability and security-smell detection powered by SpotBugs + FindSecBugs"
          comingSoon
        />
      )}

      <DashboardSection
        title="AI Recommendations"
        subtitle="Personalized improvement suggestions powered by AI agents"
        comingSoon
      />
    </div>
  );
}
