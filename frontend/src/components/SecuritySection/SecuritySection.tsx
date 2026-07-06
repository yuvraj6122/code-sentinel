import type { SecurityAnalysis } from '../../types/api';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { FindingsAccordion } from '../FindingsAccordion/FindingsAccordion';
import { MetricCard } from '../MetricCard/MetricCard';
import styles from './SecuritySection.module.css';

interface SecuritySectionProps {
  security: SecurityAnalysis;
}

export function SecuritySection({ security }: SecuritySectionProps) {
  return (
    <DashboardSection
      title="Security Analysis"
      subtitle="Vulnerability and security-smell detection powered by SpotBugs + FindSecBugs"
    >
      <div className={styles.grid}>
        <MetricCard label="Total Findings" value={security.totalFindings} mono />
        <MetricCard label="High Severity" value={security.highSeverity} mono />
        <MetricCard label="Medium Severity" value={security.mediumSeverity} mono />
        <MetricCard label="Low Severity" value={security.lowSeverity} mono />
      </div>

      <FindingsAccordion
        findings={security.findings}
        emptyMessage="No security issues detected by SpotBugs + FindSecBugs."
      />
    </DashboardSection>
  );
}
