export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message?: string;
}

export interface FieldErrorDetail {
  field: string;
  rejectedValue?: unknown;
  message: string;
}

export interface ErrorDetail {
  code: string;
  message: string;
  details?: FieldErrorDetail[];
}

export interface ApiError {
  success: false;
  error: ErrorDetail;
  timestamp: string;
  path?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
