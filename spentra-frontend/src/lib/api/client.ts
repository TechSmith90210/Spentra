/**
 * @file client.ts
 * @description Centralized HTTP client for the Spentra backend API.
 *
 * All API modules (auth, transactions, categories, budgets) delegate their
 * HTTP calls through `apiClient` which handles:
 *   - Base URL resolution from environment variable
 *   - Automatic Bearer-token injection from localStorage
 *   - Structured error handling via `ApiError`
 *   - Automatic 401 session expiration → redirect to /login
 */

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
  options: RequestInit = {},
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;

  /* ── Build headers ──────────────────────────────────────────────────────── */
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };

  // Inject Bearer token when running on the client
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('spentra-token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  /* ── Execute request ────────────────────────────────────────────────────── */
  const response = await fetch(url, {
    ...options,
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

    // Session expired — clear credentials and bounce to login
    if (response.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('spentra-token');
      localStorage.removeItem('spentra-user');
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
