import styles from './LoadingSpinner.module.css';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  label?: string;
}

export function LoadingSpinner({ size = 'md', label }: LoadingSpinnerProps) {
  return (
    <div
      className={`${styles.spinner} ${styles[size]}`}
      role="status"
      aria-label={label ?? 'Loading'}
    >
      <div className={styles.ring} aria-hidden="true" />
      {label && <span className={styles.label}>{label}</span>}
    </div>
  );
}
