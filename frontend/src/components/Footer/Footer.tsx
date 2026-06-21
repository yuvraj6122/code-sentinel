import styles from './Footer.module.css';

export function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <p className={styles.copyright}>
          &copy; {year} CodeSentinel. AI-powered code quality analysis.
        </p>
        <nav className={styles.links} aria-label="Footer navigation">
          <a
            href="https://github.com"
            className={styles.link}
            target="_blank"
            rel="noopener noreferrer"
          >
            GitHub
          </a>
        </nav>
      </div>
    </footer>
  );
}
