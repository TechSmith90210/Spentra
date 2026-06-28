/**
 * @file categories.ts
 * @description Category CRUD operations via the Spentra API.
 */

import { apiClient } from './client';
import type { Category, CreateCategoryRequest } from './types';

/** Fetch all categories for the authenticated user. */
export async function getCategories(): Promise<Category[]> {
  return apiClient<Category[]>('/api/categories');
}

/** Create a new category. */
export async function createCategory(request: CreateCategoryRequest): Promise<Category> {
  return apiClient<Category>('/api/categories', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/** Update an existing category's name. */
export async function updateCategory(id: string, request: CreateCategoryRequest): Promise<Category> {
  return apiClient<Category>(`/api/categories/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

/** Delete a category by ID. */
export async function deleteCategory(id: string): Promise<void> {
  return apiClient<void>(`/api/categories/${id}`, {
    method: 'DELETE',
  });
}
