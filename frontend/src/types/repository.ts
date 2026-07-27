export type AnalysisState = 'idle' | 'loading' | 'success' | 'error';

export interface AnalysisResult {
  analysisId: number;
  metadata: import('./api').RepositoryMetadata;
  duplication: import('./api').DuplicateCodeAnalysis | null;
  complexity: import('./api').ComplexityAnalysis | null;
  testing: import('./api').TestingAnalysis | null;
  security: import('./api').SecurityAnalysis | null;
  analyzedUrl: string;
}
