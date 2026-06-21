import styles from './Header.module.css';

export function Header() {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <a href="/" className={styles.brand} aria-label="CodeSentinel home">
          <img src="/favicon.svg" alt="" className={styles.logo} />
          <div className={styles.brandText}>
            <span className={styles.brandName}>CodeSentinel</span>
            <span className={styles.brandTag}>Code Quality Platform</span>
          </div>
        </a>

        <nav className={styles.nav} aria-label="Main navigation">
          <span className={styles.statusLabel}>
            <span className={styles.statusDot} aria-hidden="true" />
            Beta
          </span>
        </nav>
      </div>
    </header>
  );
}
