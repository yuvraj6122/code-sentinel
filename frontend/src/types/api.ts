export interface CloneRepositoryRequest {
  githubUrl: string;
}

export interface RepositoryMetadata {
  repositoryName: string;
  language: string;
  buildTool: string;
  javaFileCount: number;
  testFileCount: number;
}

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Finding {
  agentType: string;
  severity: Severity;
  title: string;
  description: string;
  filePath: string | null;
  lineNumber?: number | null;
}

export interface DuplicateCodeAnalysis {
  analysisId: number;
  totalFindings: number;
  highSeverity: number;
  mediumSeverity: number;
  lowSeverity: number;
  findings: Finding[];
}

export interface ComplexityAnalysis {
  analysisId: number;
  totalFindings: number;
  highSeverity: number;
  mediumSeverity: number;
  lowSeverity: number;
  findings: Finding[];
}

export interface TestingAnalysis {
  analysisId: number;
  totalFindings: number;
  highSeverity: number;
  mediumSeverity: number;
  lowSeverity: number;
  testingFrameworks: string[];
  mockingFrameworks: string[];
  hasTests: boolean;
  hasIntegrationTests: boolean;
  testClassCount: number;
  testMethodCount: number;
  disabledTestCount: number;
  maturityScore: number;
  maturitySummary: string;
  findings: Finding[];
}

export interface SecurityAnalysis {
  analysisId: number;
  totalFindings: number;
  highSeverity: number;
  mediumSeverity: number;
  lowSeverity: number;
  findings: Finding[];
}

export interface UnifiedTestingMetrics {
  testingFrameworks: string[];
  mockingFrameworks: string[];
  hasTests: boolean;
  hasIntegrationTests: boolean;
  testClassCount: number;
  testMethodCount: number;
  disabledTestCount: number;
  maturityScore: number;
  maturitySummary: string;
}

export interface UnifiedAnalysis {
  analysisId: number;
  repository: string;
  repositoryName: string;
  metadata: RepositoryMetadata;
  status: string;
  totalFindings: number;
  criticalSeverity: number;
  highSeverity: number;
  mediumSeverity: number;
  lowSeverity: number;
  findingsByCategory: Record<string, number>;
  findings: Finding[];
  testingMetrics: UnifiedTestingMetrics | null;
}

export type RecommendationImpact = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Recommendation {
  priority: number;
  title: string;
  description: string;
  reason: string;
  impact: RecommendationImpact;
  affectedCategories: string[];
}

export interface RecommendationReport {
  analysisId: number;
  overallAssessment: string | null;
  generatedAt: string | null;
  totalRecommendations: number;
  recommendations: Recommendation[];
}

export interface ApiErrorResponse {
  status: 'FAILED';
  localPath: null;
  message: string;
}

export class ApiError extends Error {
  readonly statusCode: number;

  constructor(message: string, statusCode: number) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
  }
}

export function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    (value as ApiErrorResponse).status === 'FAILED' &&
    'message' in value &&
    typeof (value as ApiErrorResponse).message === 'string'
  );
}

export function isRepositoryMetadata(value: unknown): value is RepositoryMetadata {
  return (
    typeof value === 'object' &&
    value !== null &&
    'repositoryName' in value &&
    'language' in value &&
    'buildTool' in value &&
    'javaFileCount' in value &&
    'testFileCount' in value
  );
}

export function isDuplicateCodeAnalysis(
  value: unknown,
): value is DuplicateCodeAnalysis {
  return (
    typeof value === 'object' &&
    value !== null &&
    'totalFindings' in value &&
    'highSeverity' in value &&
    'findings' in value &&
    Array.isArray((value as DuplicateCodeAnalysis).findings)
  );
}

export function isComplexityAnalysis(
  value: unknown,
): value is ComplexityAnalysis {
  return (
    typeof value === 'object' &&
    value !== null &&
    'totalFindings' in value &&
    'highSeverity' in value &&
    'findings' in value &&
    Array.isArray((value as ComplexityAnalysis).findings)
  );
}

export function isTestingAnalysis(value: unknown): value is TestingAnalysis {
  return (
    typeof value === 'object' &&
    value !== null &&
    'maturityScore' in value &&
    'testingFrameworks' in value &&
    'findings' in value &&
    Array.isArray((value as TestingAnalysis).findings)
  );
}

export function isSecurityAnalysis(value: unknown): value is SecurityAnalysis {
  return (
    typeof value === 'object' &&
    value !== null &&
    'totalFindings' in value &&
    'highSeverity' in value &&
    'findings' in value &&
    Array.isArray((value as SecurityAnalysis).findings)
  );
}

export function isUnifiedAnalysis(value: unknown): value is UnifiedAnalysis {
  return (
    typeof value === 'object' &&
    value !== null &&
    'analysisId' in value &&
    typeof (value as UnifiedAnalysis).analysisId === 'number' &&
    'findings' in value &&
    Array.isArray((value as UnifiedAnalysis).findings) &&
    'metadata' in value &&
    isRepositoryMetadata((value as UnifiedAnalysis).metadata)
  );
}

export function isRecommendationReport(
  value: unknown,
): value is RecommendationReport {
  return (
    typeof value === 'object' &&
    value !== null &&
    'analysisId' in value &&
    'recommendations' in value &&
    Array.isArray((value as RecommendationReport).recommendations)
  );
}
