const GITHUB_URL_PATTERN =
  /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/?(\.git)?$/;

export function isValidGitHubUrl(url: string): boolean {
  return GITHUB_URL_PATTERN.test(url.trim());
}

export function getGitHubUrlError(url: string): string | null {
  const trimmed = url.trim();

  if (!trimmed) {
    return 'GitHub repository URL is required';
  }

  if (!isValidGitHubUrl(trimmed)) {
    return 'Enter a valid GitHub URL (e.g. https://github.com/owner/repo)';
  }

  return null;
}
