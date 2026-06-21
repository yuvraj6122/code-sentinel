import { useState, type FormEvent } from 'react';
import { LoadingSpinner } from '../LoadingSpinner/LoadingSpinner';
import { getGitHubUrlError } from '../../utils/validation';
import styles from './RepositoryForm.module.css';

interface RepositoryFormProps {
  onSubmit: (githubUrl: string) => void;
  isLoading: boolean;
}

export function RepositoryForm({ onSubmit, isLoading }: RepositoryFormProps) {
  const [url, setUrl] = useState('');
  const [fieldError, setFieldError] = useState<string | null>(null);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();

    const validationError = getGitHubUrlError(url);
    if (validationError) {
      setFieldError(validationError);
      return;
    }

    setFieldError(null);
    onSubmit(url.trim());
  };

  const handleChange = (value: string) => {
    setUrl(value);
    if (fieldError) {
      setFieldError(null);
    }
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      <div
        className={`${styles.inputGroup} ${fieldError ? styles.inputGroupError : ''}`}
      >
        <div className={styles.inputWrapper}>
          <svg
            className={styles.inputIcon}
            viewBox="0 0 24 24"
            fill="currentColor"
            aria-hidden="true"
          >
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.395-.135-.345-.72-1.395-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
          </svg>
          <input
            type="url"
            className={styles.input}
            placeholder="https://github.com/owner/repository"
            value={url}
            onChange={(e) => handleChange(e.target.value)}
            disabled={isLoading}
            aria-label="GitHub repository URL"
            aria-invalid={fieldError ? true : undefined}
            aria-describedby={fieldError ? 'url-error' : undefined}
          />
        </div>
        <button type="submit" className={styles.button} disabled={isLoading}>
          {isLoading ? (
            <>
              <LoadingSpinner size="sm" />
              Analyzing…
            </>
          ) : (
            'Analyze Repository'
          )}
        </button>
      </div>

      {fieldError && (
        <p className={styles.fieldError} id="url-error" role="alert">
          {fieldError}
        </p>
      )}

      <p className={styles.hint}>
        Enter a public GitHub repository URL to scan for code quality metrics
      </p>
    </form>
  );
}
