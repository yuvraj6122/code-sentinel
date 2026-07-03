import type { Severity, TestingAnalysis } from '../../types/api';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { MetricCard } from '../MetricCard/MetricCard';
import styles from './TestingSection.module.css';

interface TestingSectionProps {
  testing: TestingAnalysis;
}

const SEVERITY_CLASS: Record<Severity, string> = {
  LOW: styles.sevLow,
  MEDIUM: styles.sevMedium,
  HIGH: styles.sevHigh,
  CRITICAL: styles.sevCritical,
};

const SEVERITY_ORDER: Record<Severity, number> = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
};

function maturityClass(score: number): string {
  if (score >= 80) return styles.maturityHigh;
  if (score >= 40) return styles.maturityMedium;
  return styles.maturityLow;
}

export function TestingSection({ testing }: TestingSectionProps) {
  const sortedFindings = [...testing.findings].sort(
    (a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity],
  );

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

      {sortedFindings.length === 0 ? (
        <div className={styles.empty}>
          <svg viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path
              fillRule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clipRule="evenodd"
            />
          </svg>
          No testing quality issues detected.
        </div>
      ) : (
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th className={styles.severityCol}>Severity</th>
                <th className={styles.titleCol}>Issue</th>
                <th>Description</th>
                <th className={styles.fileCol}>File</th>
              </tr>
            </thead>
            <tbody>
              {sortedFindings.map((finding, index) => (
                <tr key={`${finding.filePath ?? 'finding'}-${index}`}>
                  <td>
                    <span
                      className={`${styles.badge} ${SEVERITY_CLASS[finding.severity]}`}
                    >
                      {finding.severity}
                    </span>
                  </td>
                  <td className={styles.title}>{finding.title}</td>
                  <td className={styles.description}>{finding.description}</td>
                  <td className={styles.file}>{finding.filePath ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardSection>
  );
}
