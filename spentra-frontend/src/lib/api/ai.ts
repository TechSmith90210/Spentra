/**
 * @file ai.ts
 * @description AI assistant API operations via the Spentra backend.
 */

import { apiClient } from './client';
import type { AiSummaryResponse, TransactionDraft } from './types';

export const MAX_RECEIPT_SIZE_BYTES = 2 * 1024 * 1024;
export const RECEIPT_SIZE_ERROR = 'Receipt images must be 2 MB or smaller.';

export async function parseTextPrompt(prompt: string): Promise<TransactionDraft> {
  return apiClient<TransactionDraft>('/api/ai/parse-text', {
    method: 'POST',
    body: JSON.stringify({ prompt }),
  });
}

export async function getAiInsights(month: string, currency: string): Promise<AiSummaryResponse> {
  return apiClient<AiSummaryResponse>(`/api/ai/insights?month=${month}&currency=${currency}`);
}

export async function refreshAiInsights(month: string, currency: string): Promise<AiSummaryResponse> {
  return apiClient<AiSummaryResponse>(`/api/ai/insights/refresh?month=${month}&currency=${currency}`, {
    method: 'POST',
  });
}

export async function parseReceiptImage(file: File): Promise<TransactionDraft> {
  if (file.size > MAX_RECEIPT_SIZE_BYTES) {
    throw new Error(RECEIPT_SIZE_ERROR);
  }

  const formData = new FormData();
  formData.append('file', file);

  return apiClient<TransactionDraft>('/api/ai/parse-receipt', {
    method: 'POST',
    body: formData,
  });
}
