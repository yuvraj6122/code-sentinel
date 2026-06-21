import type { ReactNode } from 'react';
import styles from './DashboardSection.module.css';

interface DashboardSectionProps {
  title: string;
  subtitle?: string;
  comingSoon?: boolean;
  children?: ReactNode;
}

export function DashboardSection({
  title,
  subtitle,
  comingSoon = false,
  children,
}: DashboardSectionProps) {
  return (
    <section className={styles.section} aria-labelledby={`section-${title}`}>
      <div className={styles.header}>
        <div>
          <div className={styles.titleGroup}>
            <h2 className={styles.title} id={`section-${title}`}>
              {title}
            </h2>
            {comingSoon && (
              <span className={`${styles.badge} ${styles.comingSoon}`}>
                Coming Soon
              </span>
            )}
          </div>
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
        </div>
      </div>

      <div className={styles.content}>
        {comingSoon ? (
          <div className={styles.overlay}>
            <svg
              className={styles.overlayIcon}
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z"
                clipRule="evenodd"
              />
            </svg>
            Feature in development
          </div>
        ) : (
          children
        )}
      </div>
    </section>
  );
}
