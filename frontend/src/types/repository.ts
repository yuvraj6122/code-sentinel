export type AnalysisState = 'idle' | 'loading' | 'success' | 'error';

export interface AnalysisResult {
  metadata: import('./api').RepositoryMetadata;
  duplication: import('./api').DuplicateCodeAnalysis | null;
  analyzedUrl: string;
}
