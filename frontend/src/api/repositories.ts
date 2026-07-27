import {
  ApiError,
  isApiErrorResponse,
  isComplexityAnalysis,
  isDuplicateCodeAnalysis,
  isRecommendationReport,
  isRepositoryMetadata,
  isSecurityAnalysis,
  isTestingAnalysis,
  isUnifiedAnalysis,
  type CloneRepositoryRequest,
  type ComplexityAnalysis,
  type DuplicateCodeAnalysis,
  type RecommendationReport,
  type RepositoryMetadata,
  type SecurityAnalysis,
  type TestingAnalysis,
  type UnifiedAnalysis,
} from '../types/api';

const API_BASE = '/api';

export async function analyzeRepository(
  request: CloneRepositoryRequest,
): Promise<RepositoryMetadata> {
  const response = await fetch(`${API_BASE}/repositories/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isRepositoryMetadata(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function analyzeDuplication(
  request: CloneRepositoryRequest,
): Promise<DuplicateCodeAnalysis> {
  const response = await fetch(`${API_BASE}/duplication/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isDuplicateCodeAnalysis(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function analyzeComplexity(
  request: CloneRepositoryRequest,
): Promise<ComplexityAnalysis> {
  const response = await fetch(`${API_BASE}/complexity/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isComplexityAnalysis(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function analyzeTesting(
  request: CloneRepositoryRequest,
): Promise<TestingAnalysis> {
  const response = await fetch(`${API_BASE}/testing/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isTestingAnalysis(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function analyzeSecurity(
  request: CloneRepositoryRequest,
): Promise<SecurityAnalysis> {
  const response = await fetch(`${API_BASE}/security/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isSecurityAnalysis(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function runUnifiedAnalysis(
  request: CloneRepositoryRequest,
): Promise<UnifiedAnalysis> {
  const response = await fetch(`${API_BASE}/analysis/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isUnifiedAnalysis(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function generateRecommendations(
  analysisId: number,
  force = false,
): Promise<RecommendationReport> {
  const response = await fetch(
    `${API_BASE}/analyses/${analysisId}/recommendations?force=${force}`,
    { method: 'POST', headers: { 'Content-Type': 'application/json' } },
  );

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isRecommendationReport(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function getRecommendations(
  analysisId: number,
): Promise<RecommendationReport> {
  const response = await fetch(
    `${API_BASE}/analyses/${analysisId}/recommendations`,
  );

  const body: unknown = await response.json();

  if (!response.ok) {
    const message = isApiErrorResponse(body)
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  if (!isRecommendationReport(body)) {
    throw new ApiError('Unexpected response from server', response.status);
  }

  return body;
}

export async function checkHealth(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE}/health`);
    return response.ok;
  } catch {
    return false;
  }
}
