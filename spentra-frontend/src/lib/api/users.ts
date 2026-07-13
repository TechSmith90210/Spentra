/**
 * @file users.ts
 * @description Client API module for managing user profile (name, avatar).
 */

import { apiClient } from './client';

export interface UserProfileResponse {
  id: string;
  name: string;
  email: string;
  profilePic?: string;
}

export interface UpdateProfileRequest {
  name?: string;
  profilePic?: string;
}

/**
 * Fetch the authenticated user's profile from the backend.
 */
export async function getProfile(): Promise<UserProfileResponse> {
  return apiClient<UserProfileResponse>('/api/users/profile');
}

/**
 * Update the authenticated user's name and/or profile picture.
 */
export async function updateProfile(
  data: UpdateProfileRequest,
): Promise<UserProfileResponse> {
  return apiClient<UserProfileResponse>('/api/users/profile', {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}
