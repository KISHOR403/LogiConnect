/**
 * Isolated token storage interface.
 * Encapsulates browser storage handling to allow future migration to in-memory/cookie storage
 * without touching UI or business logic.
 * NEVER logs tokens or outputs token values.
 */

const ACCESS_TOKEN_KEY = 'lc_auth_access';
const REFRESH_TOKEN_KEY = 'lc_auth_refresh';

export const tokenStorage = {
  getAccessToken(): string | null {
    try {
      return localStorage.getItem(ACCESS_TOKEN_KEY);
    } catch {
      return null;
    }
  },

  setAccessToken(token: string): void {
    try {
      if (token) {
        localStorage.setItem(ACCESS_TOKEN_KEY, token);
      }
    } catch {
      // Ignore storage quota/permission failures safely
    }
  },

  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_TOKEN_KEY);
    } catch {
      return null;
    }
  },

  setRefreshToken(token: string): void {
    try {
      if (token) {
        localStorage.setItem(REFRESH_TOKEN_KEY, token);
      }
    } catch {
      // Ignore storage quota/permission failures safely
    }
  },

  setTokens(accessToken: string, refreshToken?: string): void {
    this.setAccessToken(accessToken);
    if (refreshToken) {
      this.setRefreshToken(refreshToken);
    }
  },

  clearTokens(): void {
    try {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    } catch {
      // Safe fallback
    }
  },

  hasValidToken(): boolean {
    return Boolean(this.getAccessToken());
  },
};
