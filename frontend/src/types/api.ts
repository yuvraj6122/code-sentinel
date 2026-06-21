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
