import type { Finding, Severity } from '../../types/api';
import styles from './FindingsAccordion.module.css';

interface FindingsAccordionProps {
  findings: Finding[];
  emptyMessage: string;
}

const SEVERITY_CLASS: Record<Severity, string> = {
  LOW: styles.sevLow,
  MEDIUM: styles.sevMedium,
  HIGH: styles.sevHigh,
  CRITICAL: styles.sevCritical,
};

const SEVERITY_ORDER: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export function FindingsAccordion({ findings, emptyMessage }: FindingsAccordionProps) {
  if (findings.length === 0) {
    return (
      <div className={styles.empty}>
        <svg viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path
            fillRule="evenodd"
            d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
            clipRule="evenodd"
          />
        </svg>
        {emptyMessage}
      </div>
    );
  }

  const groups = SEVERITY_ORDER.map((severity) => ({
    severity,
    items: findings.filter((finding) => finding.severity === severity),
  })).filter((group) => group.items.length > 0);

  return (
    <div className={styles.list}>
      {groups.map(({ severity, items }) => (
        <details
          key={severity}
          className={styles.group}
          open={severity === 'CRITICAL' || severity === 'HIGH'}
        >
          <summary className={styles.summary}>
            <span className={`${styles.badge} ${SEVERITY_CLASS[severity]}`}>
              {severity}
            </span>
            <span className={styles.count}>
              {items.length} {items.length === 1 ? 'issue' : 'issues'}
            </span>
            <svg
              className={styles.chevron}
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </summary>
          <ul className={styles.body}>
            {items.map((finding, index) => (
              <li
                key={`${finding.filePath ?? 'finding'}-${index}`}
                className={styles.issue}
              >
                <p className={styles.title}>{finding.title}</p>
                <p className={styles.description}>{finding.description}</p>
                {finding.filePath && (
                  <p className={styles.file}>
                    <span className={styles.fileLabel}>File</span>
                    {finding.filePath}
                    {finding.lineNumber != null && `:${finding.lineNumber}`}
                  </p>
                )}
              </li>
            ))}
          </ul>
        </details>
      ))}
    </div>
  );
}
