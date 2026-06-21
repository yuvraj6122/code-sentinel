import {
  ApiError,
  isApiErrorResponse,
  isRepositoryMetadata,
  type CloneRepositoryRequest,
  type RepositoryMetadata,
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

export async function checkHealth(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE}/health`);
    return response.ok;
  } catch {
    return false;
  }
}
