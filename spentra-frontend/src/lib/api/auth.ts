/**
 * @file auth.ts
 * @description Authentication API functions for login and signup.
 */

import { apiClient } from './client';
import type { AuthRequest, AuthResponse, SignUpRequest } from './types';

/** Authenticate an existing user with email and password. */
export async function login(request: AuthRequest): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/** Register a new user account. */
export async function signup(request: SignUpRequest): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/signUp', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/** Authenticate or sign up a user with a Google ID Token. */
export async function googleLogin(idToken: string): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken }),
  });
}
