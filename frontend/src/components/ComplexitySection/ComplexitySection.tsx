import type { ComplexityAnalysis } from '../../types/api';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { FindingsAccordion } from '../FindingsAccordion/FindingsAccordion';
import { MetricCard } from '../MetricCard/MetricCard';
import styles from './ComplexitySection.module.css';

interface ComplexitySectionProps {
  complexity: ComplexityAnalysis;
}

export function ComplexitySection({ complexity }: ComplexitySectionProps) {
  return (
    <DashboardSection
      title="Complexity Analysis"
      subtitle="Cyclomatic complexity and method/class length checks powered by PMD"
    >
      <div className={styles.grid}>
        <MetricCard label="Total Findings" value={complexity.totalFindings} mono />
        <MetricCard label="High Severity" value={complexity.highSeverity} mono />
        <MetricCard label="Medium Severity" value={complexity.mediumSeverity} mono />
        <MetricCard label="Low Severity" value={complexity.lowSeverity} mono />
      </div>

      <FindingsAccordion
        findings={complexity.findings}
        emptyMessage="No overly complex code detected above the reporting threshold."
      />
    </DashboardSection>
  );
}
