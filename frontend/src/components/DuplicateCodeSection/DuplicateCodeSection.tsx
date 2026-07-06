import type { DuplicateCodeAnalysis } from '../../types/api';
import { DashboardSection } from '../DashboardSection/DashboardSection';
import { FindingsAccordion } from '../FindingsAccordion/FindingsAccordion';
import { MetricCard } from '../MetricCard/MetricCard';
import styles from './DuplicateCodeSection.module.css';

interface DuplicateCodeSectionProps {
  duplication: DuplicateCodeAnalysis;
}

export function DuplicateCodeSection({ duplication }: DuplicateCodeSectionProps) {
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

      <FindingsAccordion
        findings={duplication.findings}
        emptyMessage="No duplicate code detected above the reporting threshold."
      />
    </DashboardSection>
  );
}
