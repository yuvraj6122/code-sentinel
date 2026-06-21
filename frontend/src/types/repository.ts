export type AnalysisState = 'idle' | 'loading' | 'success' | 'error';

export interface AnalysisResult {
  metadata: import('./api').RepositoryMetadata;
  analyzedUrl: string;
}
