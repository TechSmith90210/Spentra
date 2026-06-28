/**
 * @file budgets.ts
 * @description Budget management operations via the Spentra API.
 *
 * Budgets set monthly spending limits, optionally scoped to a category.
 * The summary endpoint returns actual-vs-limit analytics.
 */

import { apiClient } from './client';
import type { Budget, BudgetSummary, CreateBudgetRequest } from './types';

/**
 * Create or update a budget for a given month/category.
 *
 * @param request - Budget creation payload (amount limit, optional category & month)
 * @returns The created/updated budget record
 */
export async function setBudget(
  request: CreateBudgetRequest,
): Promise<Budget> {
  return apiClient<Budget>('/api/budgets', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Fetch budget summaries with spend analytics.
 *
 * @param month - Optional month in `YYYY-MM` format. Defaults to current month on the backend.
 * @returns Array of budget summaries with actual spend, remaining, and exceeded flags
 */
export async function getBudgetSummary(
  month?: string,
): Promise<BudgetSummary[]> {
  const query = month ? `?month=${encodeURIComponent(month)}` : '';
  return apiClient<BudgetSummary[]>(`/api/budgets/summary${query}`, {
    method: 'GET',
  });
}
