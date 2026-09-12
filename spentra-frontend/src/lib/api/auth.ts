/**
 * @file auth.ts
 * @description Authentication API functions for login and signup.
 */

import { ApiError, apiClient } from './client';
import type { AuthRequest, AuthResponse, SignUpRequest } from './types';

export const INVALID_CREDENTIALS_LOGIN_ERROR =
  'Invalid email or password. Please try again.';

/** Returns the safe, user-facing message for an email/password login failure. */
export function getLoginErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.statusCode === 401) {
    return INVALID_CREDENTIALS_LOGIN_ERROR;
  }

  return error instanceof Error ? error.message : 'Login failed. Please try again.';
}

/** Authenticate an existing user with email and password. */
export async function login(request: AuthRequest): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
    includeAuthToken: false,
  });
}

/** Register a new user account. */
export async function signup(request: SignUpRequest): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/signUp', {
    method: 'POST',
    body: JSON.stringify(request),
    includeAuthToken: false,
  });
}

/** Authenticate or sign up a user with a Google ID Token. */
export async function googleLogin(idToken: string): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken }),
    includeAuthToken: false,
  });
}
