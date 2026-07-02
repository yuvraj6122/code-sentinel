import type { DuplicateCodeAnalysis, Severity } from '../../types/api';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { MetricCard } from '../MetricCard/MetricCard';
import styles from './DuplicateCodeSection.module.css';

interface DuplicateCodeSectionProps {
  duplication: DuplicateCodeAnalysis;
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

export function DuplicateCodeSection({ duplication }: DuplicateCodeSectionProps) {
  const sortedFindings = [...duplication.findings].sort(
    (a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity],
  );

  return (
    <DashboardSection
      title="Duplicate Code"
      subtitle="Copy/paste detection powered by PMD CPD"
    >
      <div className={styles.grid}>
        <MetricCard label="Total Findings" value={duplication.totalFindings} mono />
        <MetricCard label="High Severity" value={duplication.highSeverity} mono />
        <MetricCard label="Medium Severity" value={duplication.mediumSeverity} mono />
        <MetricCard label="Low Severity" value={duplication.lowSeverity} mono />
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
          No duplicate code detected above the reporting threshold.
        </div>
      ) : (
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th className={styles.severityCol}>Severity</th>
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
