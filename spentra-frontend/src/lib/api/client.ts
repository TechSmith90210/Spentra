/**
 * @file client.ts
 * @description Centralized HTTP client for the Spentra backend API.
 *
 * All API modules (auth, transactions, categories, budgets) delegate their
 * HTTP calls through `apiClient` which handles:
 *   - Base URL resolution from environment variable
 *   - Automatic Bearer-token injection from localStorage
 *   - Structured error handling via `ApiError`
 *   - Automatic 401 session expiration on authenticated requests → redirect to /login
 */

import { SPENTRA_TOKEN_KEY, SPENTRA_USER_KEY } from '@/lib/constants/auth';
import type { ApiErrorResponse } from './types';

/* ─── Configuration ─────────────────────────────────────────────────────────── */

/** Backend base URL — falls back to the deployed Render service */
const API_BASE_URL: string =
  process.env.NEXT_PUBLIC_API_BASE_URL || 'https://spentra-backend.onrender.com';

/* ─── Custom Error ──────────────────────────────────────────────────────────── */

/**
 * Structured API error that preserves the backend's error payload.
 *
 * @example
 * ```ts
 * try {
 *   await apiClient('/transactions');
 * } catch (err) {
 *   if (err instanceof ApiError && err.statusCode === 404) { ... }
 * }
 * ```
 */
export class ApiError extends Error {
  /** HTTP status code returned by the server */
  public readonly statusCode: number;

  /** ISO-8601 timestamp from the server error response */
  public readonly timestamp: string;

  constructor(message: string, statusCode: number, timestamp: string) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
    this.timestamp = timestamp;
  }
}

/** Options accepted by {@link apiClient} in addition to standard fetch options. */
export interface ApiClientOptions extends RequestInit {
  /**
   * Whether the current session token may be sent with the request.
   *
   * Authentication endpoints must opt out so an invalid login attempt is not
   * treated as an expired session when a stale token exists in localStorage.
   */
  includeAuthToken?: boolean;
}

/* ─── Client ────────────────────────────────────────────────────────────────── */

/**
 * Generic fetch wrapper that prepends the API base URL, injects auth headers,
 * and normalizes error responses into `ApiError` instances.
 *
 * @typeParam T - Expected shape of the successful JSON response body
 * @param endpoint - API path (e.g. `/api/transactions`)
 * @param options  - Standard `RequestInit` overrides (method, body, headers, …)
 * @returns Parsed JSON response cast to `T`
 *
 * @throws {ApiError} When the response status is not OK
 */
export async function apiClient<T>(
  endpoint: string,
  options: ApiClientOptions = {},
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  const {
    includeAuthToken = true,
    headers: requestHeaders,
    ...requestOptions
  } = options;

  /* ── Build headers ──────────────────────────────────────────────────────── */
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
  const headers: Record<string, string> = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(requestHeaders as Record<string, string> | undefined),
  };

  // Public auth requests must never inherit a stale session token.
  if (!includeAuthToken) {
    delete headers.Authorization;
  } else if (typeof window !== 'undefined') {
    // Inject Bearer token when running on the client.
    const token = localStorage.getItem(SPENTRA_TOKEN_KEY);
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const isAuthenticatedRequest = Boolean(headers.Authorization);

  /* ── Execute request ────────────────────────────────────────────────────── */
  const response = await fetch(url, {
    ...requestOptions,
    headers,
  });

  /* ── Handle errors ──────────────────────────────────────────────────────── */
  if (!response.ok) {
    // Attempt to parse a structured error body from the backend
    let errorBody: ApiErrorResponse;
    try {
      errorBody = (await response.json()) as ApiErrorResponse;
    } catch {
      errorBody = {
        message: response.statusText || 'An unexpected error occurred',
        statusCode: response.status,
        timestamp: new Date().toISOString(),
      };
    }

    // Session expired — clear credentials and bounce to login. Public login
    // failures stay on the form so users can correct and resubmit it.
    if (
      response.status === 401 &&
      isAuthenticatedRequest &&
      typeof window !== 'undefined'
    ) {
      localStorage.removeItem(SPENTRA_TOKEN_KEY);
      localStorage.removeItem(SPENTRA_USER_KEY);
      window.location.href = '/login';
    }

    throw new ApiError(
      errorBody.message,
      errorBody.statusCode,
      errorBody.timestamp,
    );
  }

  /* ── Parse response ─────────────────────────────────────────────────────── */
  // Handle 204 No Content gracefully
  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
