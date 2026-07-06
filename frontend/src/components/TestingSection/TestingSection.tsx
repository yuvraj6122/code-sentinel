import type { TestingAnalysis } from '../../types/api';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { FindingsAccordion } from '../FindingsAccordion/FindingsAccordion';
import { MetricCard } from '../MetricCard/MetricCard';
import styles from './TestingSection.module.css';

interface TestingSectionProps {
  testing: TestingAnalysis;
}

function maturityClass(score: number): string {
  if (score >= 80) return styles.maturityHigh;
  if (score >= 40) return styles.maturityMedium;
  return styles.maturityLow;
}

export function TestingSection({ testing }: TestingSectionProps) {
  const score = testing.maturityScore;

  return (
    <DashboardSection
      title="Testing Analysis"
      subtitle="Test-suite quality and maturity — frameworks, integration coverage, disabled tests, assertions, naming, and organization"
    >
      <div className={styles.maturity}>
        <div className={styles.maturityHeader}>
          <p className={styles.maturityLabel}>Testing Maturity Score</p>
          <p className={styles.maturityValue}>
            <span className={styles.maturityNumber}>{score}</span>
            <span className={styles.maturityOutOf}>/ 100</span>
            <span className={`${styles.maturityBadge} ${maturityClass(score)}`}>
              {testing.maturitySummary}
            </span>
          </p>
        </div>
        <div className={styles.maturityBar}>
          <div
            className={`${styles.maturityFill} ${maturityClass(score)}`}
            style={{ width: `${Math.min(Math.max(score, 0), 100)}%` }}
          />
        </div>
      </div>

      <div className={styles.grid}>
        <MetricCard label="Test Classes" value={testing.testClassCount} mono />
        <MetricCard label="Test Methods" value={testing.testMethodCount} mono />
        <MetricCard label="Disabled Tests" value={testing.disabledTestCount} mono />
        <MetricCard
          label="Integration Tests"
          value={testing.hasIntegrationTests ? 'Yes' : 'No'}
          highlight={testing.hasIntegrationTests}
        />
      </div>

      <div className={styles.frameworks}>
        <div className={styles.frameworkGroup}>
          <span className={styles.frameworkTitle}>Testing Frameworks</span>
          <div className={styles.chips}>
            {testing.testingFrameworks.length > 0 ? (
              testing.testingFrameworks.map((name) => (
                <span key={name} className={styles.chip}>
                  {name}
                </span>
              ))
            ) : (
              <span className={styles.chipMuted}>None detected</span>
            )}
          </div>
        </div>
        <div className={styles.frameworkGroup}>
          <span className={styles.frameworkTitle}>Mocking Frameworks</span>
          <div className={styles.chips}>
            {testing.mockingFrameworks.length > 0 ? (
              testing.mockingFrameworks.map((name) => (
                <span key={name} className={styles.chip}>
                  {name}
                </span>
              ))
            ) : (
              <span className={styles.chipMuted}>None detected</span>
            )}
          </div>
        </div>
      </div>

      <div className={styles.grid}>
        <MetricCard label="Total Findings" value={testing.totalFindings} mono />
        <MetricCard label="High Severity" value={testing.highSeverity} mono />
        <MetricCard label="Medium Severity" value={testing.mediumSeverity} mono />
        <MetricCard label="Low Severity" value={testing.lowSeverity} mono />
      </div>

      <FindingsAccordion
        findings={testing.findings}
        emptyMessage="No testing quality issues detected."
      />
    </DashboardSection>
  );
}
