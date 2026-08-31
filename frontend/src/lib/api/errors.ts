import axios, { AxiosError } from 'axios';
import { ApiError } from '@/types/api';

export interface FormattedError {
  message: string;
  code?: string;
  status?: number;
  fieldErrors?: Record<string, string>;
}

const DEFAULT_STATUS_MESSAGES: Record<number, string> = {
  400: 'The request could not be processed. Please check your input.',
  401: 'Your session has expired or is invalid. Please sign in again.',
  403: "You don't have permission to perform this action.",
  404: 'The requested resource could not be found.',
  409: 'A conflict occurred with an existing record.',
  422: 'Invalid input provided. Please correct the highlighted errors.',
  429: 'Too many requests. Please wait a moment before trying again.',
  500: 'Something went wrong on our end. Please try again later.',
  502: 'Gateway error. The server is temporarily unavailable.',
  503: 'Service temporarily unavailable. Please try again shortly.',
};

/**
 * Strips technical database/SQL/JWT/stack traces and returns a clean, safe, human-readable message.
 */
function sanitizeErrorMessage(rawMessage?: string): string | null {
  if (!rawMessage) return null;
  const lower = rawMessage.toLowerCase();

  // Guard against leaking internal implementation details
  if (
    lower.includes('sql') ||
    lower.includes('hibernate') ||
    lower.includes('constraint') ||
    lower.includes('foreign key') ||
    lower.includes('jwt') ||
    lower.includes('bearer') ||
    lower.includes('nullpointer') ||
    lower.includes('exception') ||
    lower.includes('stacktrace') ||
    lower.includes('database')
  ) {
    return 'An internal processing error occurred. Please contact system support.';
  }

  return rawMessage;
}

export function formatApiError(error: unknown): FormattedError {
  if (!axios.isAxiosError(error)) {
    if (error instanceof Error) {
      return { message: error.message || 'An unexpected error occurred.' };
    }
    return { message: 'An unexpected error occurred. Please try again.' };
  }

  const axiosError = error as AxiosError<ApiError | unknown>;
  const status = axiosError.response?.status;
  const data = axiosError.response?.data as ApiError | undefined;

  let fieldErrors: Record<string, string> | undefined;
  if (data?.error?.details && Array.isArray(data.error.details)) {
    fieldErrors = {};
    for (const detail of data.error.details) {
      if (detail.field && detail.message) {
        fieldErrors[detail.field] = detail.message;
      }
    }
  }

  // 1. Try sanitized message from backend error envelope
  const backendMessage = sanitizeErrorMessage(data?.error?.message);
  if (backendMessage) {
    return {
      message: backendMessage,
      code: data?.error?.code,
      status,
      fieldErrors,
    };
  }

  // 2. Fall back to status code default message
  if (status && DEFAULT_STATUS_MESSAGES[status]) {
    return {
      message: DEFAULT_STATUS_MESSAGES[status],
      status,
      fieldErrors,
    };
  }

  // 3. Network / offline error
  if (axiosError.code === 'ERR_NETWORK' || !axiosError.response) {
    return {
      message: 'Unable to connect to the server. Please check your network connection.',
      code: 'NETWORK_ERROR',
    };
  }

  // 4. Default generic message
  return {
    message: 'An unexpected error occurred. Please try again.',
    status,
  };
}
