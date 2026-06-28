/**
 * @file transactions.ts
 * @description Transaction CRUD operations via the Spentra API.
 */

import { apiClient } from './client';
import type { CreateTransactionRequest, Transaction, UpdateTransactionRequest } from './types';

/** Fetch all transactions for the authenticated user. */
export async function getTransactions(): Promise<Transaction[]> {
  return apiClient<Transaction[]>('/api/expenses');
}

/** Create a new transaction. */
export async function createTransaction(request: CreateTransactionRequest): Promise<Transaction> {
  return apiClient<Transaction>('/api/expenses', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/** Update an existing transaction by ID. */
export async function updateTransaction(id: string, request: UpdateTransactionRequest): Promise<Transaction> {
  return apiClient<Transaction>(`/api/expenses/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

/** Delete a transaction by ID. */
export async function deleteTransaction(id: string): Promise<void> {
  return apiClient<void>(`/api/expenses/${id}`, {
    method: 'DELETE',
  });
}
