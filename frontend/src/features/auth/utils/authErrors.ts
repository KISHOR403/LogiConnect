import axios from 'axios';
import { ApiError } from '@/types/api';

export type AuthErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'ACCOUNT_LOCKED'
  | 'ACCOUNT_INACTIVE'
  | 'NETWORK_ERROR'
  | 'SERVER_ERROR'
  | 'VALIDATION_ERROR'
  | 'UNKNOWN_ERROR';

export interface AuthErrorDetails {
  code: AuthErrorCode;
  title: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

/**
 * Strips technical database/SQL/JWT/stack traces to prevent leaking internal details.
 */
export function sanitizeMessage(raw?: string): string | null {
  if (!raw) return null;
  const lower = raw.toLowerCase();
  const dangerousPatterns = [
    'sql',
    'hibernate',
    'constraint',
    'foreign key',
    'primary key',
    'jwt',
    'bearer',
    'nullpointer',
    'exception',
    'stacktrace',
    'database',
    'query',
    'table',
    'column',
    'syntax error',
    'org.springframework',
    'com.logiconnect',
  ];

  if (dangerousPatterns.some((pattern) => lower.includes(pattern))) {
    return null;
  }
  return raw;
}

/**
 * Categorizes and formats errors specifically for enterprise authentication workflows.
 */
export function parseAuthError(error: unknown): AuthErrorDetails {
  if (!axios.isAxiosError(error)) {
    if (error instanceof Error) {
      const sanitized = sanitizeMessage(error.message);
      return {
        code: 'UNKNOWN_ERROR',
        title: 'Authentication Error',
        message: sanitized || 'An unexpected error occurred. Please try again.',
      };
    }
    return {
      code: 'UNKNOWN_ERROR',
      title: 'Authentication Error',
      message: 'An unexpected authentication error occurred.',
    };
  }

  // Network / Offline / Connection errors
  if (error.code === 'ERR_NETWORK' || !error.response) {
    return {
      code: 'NETWORK_ERROR',
      title: 'Connection Error',
      message: 'Unable to connect to the authentication service. Please check your network connection.',
    };
  }

  const status = error.response.status;
  const data = error.response.data as ApiError | undefined;
  const rawMessage = data?.error?.message || '';
  const sanitized = sanitizeMessage(rawMessage);
  const lowerMessage = rawMessage.toLowerCase();

  // Extract validation field errors if provided by backend
  let fieldErrors: Record<string, string> | undefined;
  if (data?.error?.details && Array.isArray(data.error.details)) {
    fieldErrors = {};
    for (const detail of data.error.details) {
      if (detail.field && detail.message) {
        fieldErrors[detail.field] = detail.message;
      }
    }
  }

  // 1. Account Locked
  if (
    lowerMessage.includes('locked') ||
    data?.error?.code === 'ACCOUNT_LOCKED'
  ) {
    return {
      code: 'ACCOUNT_LOCKED',
      title: 'Account Temporarily Locked',
      message:
        sanitized ||
        'Your account has been temporarily locked due to repeated failed login attempts. Please try again later or contact your IT administrator.',
      fieldErrors,
    };
  }

  // 2. Inactive / Suspended / Disabled Account
  if (
    lowerMessage.includes('inactive') ||
    lowerMessage.includes('disabled') ||
    lowerMessage.includes('suspended') ||
    data?.error?.code === 'ACCOUNT_INACTIVE'
  ) {
    return {
      code: 'ACCOUNT_INACTIVE',
      title: 'Account Disabled',
      message:
        sanitized ||
        'Your account is currently inactive or disabled. Please contact your IT administrator.',
      fieldErrors,
    };
  }

  // 3. Invalid Credentials
  if (
    status === 401 ||
    lowerMessage.includes('invalid credentials') ||
    data?.error?.code === 'INVALID_CREDENTIALS'
  ) {
    return {
      code: 'INVALID_CREDENTIALS',
      title: 'Invalid Credentials',
      message:
        sanitized ||
        'Invalid credentials. Please check your username/email and password.',
      fieldErrors,
    };
  }

  // 4. Server error (5xx)
  if (status >= 500) {
    return {
      code: 'SERVER_ERROR',
      title: 'Service Error',
      message: 'The authentication service is temporarily unavailable. Please try again later.',
      fieldErrors,
    };
  }

  // 5. Default fallback
  return {
    code: 'UNKNOWN_ERROR',
    title: 'Authentication Failed',
    message: sanitized || 'Authentication failed. Please verify your credentials and try again.',
    fieldErrors,
  };
}
