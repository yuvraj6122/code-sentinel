import type { ReactNode } from 'react';
import styles from './MetricCard.module.css';

interface MetricCardProps {
  label: string;
  value: string | number;
  icon?: ReactNode;
  highlight?: boolean;
  mono?: boolean;
}

export function MetricCard({
  label,
  value,
  icon,
  highlight = false,
  mono = false,
}: MetricCardProps) {
  return (
    <div
      className={`${styles.card} ${highlight ? styles.highlight : ''}`}
    >
      {icon && <div className={styles.icon}>{icon}</div>}
      <p className={styles.label}>{label}</p>
      <p className={mono ? styles.valueMono : styles.value}>{value}</p>
    </div>
  );
}
