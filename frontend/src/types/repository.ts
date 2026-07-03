export type AnalysisState = 'idle' | 'loading' | 'success' | 'error';

export interface AnalysisResult {
  metadata: import('./api').RepositoryMetadata;
  duplication: import('./api').DuplicateCodeAnalysis | null;
  complexity: import('./api').ComplexityAnalysis | null;
  analyzedUrl: string;
}
