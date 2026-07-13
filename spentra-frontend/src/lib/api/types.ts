/**
 * @file types.ts
 * @description Shared TypeScript type definitions for the Spentra API layer.
 *
 * These types mirror the backend DTOs and are consumed by all API client
 * functions, provider state, and UI components.
 */

/* ─── Authentication ────────────────────────────────────────────────────────── */

/** Payload for the login endpoint */
export interface AuthRequest {
  email: string;
  password: string;
}

/** Payload for the signup endpoint — extends login with confirmation & name */
export interface SignUpRequest extends AuthRequest {
  confirmPassword: string;
  name?: string;
}

/** Successful auth response from the backend */
export interface AuthResponse {
  token: string;
  email: string;
  name: string;
  profilePic?: string;
}

/* ─── Categories ────────────────────────────────────────────────────────────── */

/** A spending category (e.g. "Food", "Transport") */
export interface Category {
  id: string;
  name: string;
}

/** Payload when creating a new category */
export interface CreateCategoryRequest {
  name: string;
}

/* ─── Transactions ──────────────────────────────────────────────────────────── */

/** Discriminator for money flow direction */
export type TransactionType = 'EXPENSE' | 'CREDIT';

/** Recurrence frequency for recurring transactions */
export type Recurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

/** A single transaction record returned by the backend */
export interface Transaction {
  id: string;
  title: string;
  amount: number;
  category: Category | null;
  type: TransactionType;
  transactionDate: string;
  isRecurring: boolean;
  recurrence: Recurrence;
  nextExecutionDate: string | null;
}

/** Payload for creating a new transaction */
export interface CreateTransactionRequest {
  title: string;
  amount: number;
  categoryId?: string;
  type?: TransactionType;
  transactionDate?: string;
  isRecurring?: boolean;
  recurrence?: Recurrence;
}

/** Payload for partially updating an existing transaction */
export interface UpdateTransactionRequest {
  title?: string;
  amount?: number;
  categoryId?: string;
  type?: TransactionType;
  transactionDate?: string;
  isRecurring?: boolean;
  recurrence?: Recurrence;
}

/* ─── Budgets ───────────────────────────────────────────────────────────────── */

/** Payload for setting / creating a budget */
export interface CreateBudgetRequest {
  amountLimit: number;
  categoryId?: string;
  budgetMonth?: string;
}

/** A budget record as stored in the backend */
export interface Budget {
  id: string;
  categoryId: string | null;
  categoryName: string;
  amountLimit: number;
  budgetMonth: string;
}

/** Budget with computed spend analytics — returned by the summary endpoint */
export interface BudgetSummary {
  budgetId: string;
  categoryId: string | null;
  categoryName: string;
  amountLimit: number;
  actualSpent: number;
  remaining: number;
  isExceeded: boolean;
  budgetMonth: string;
}

/* ─── Error Handling ────────────────────────────────────────────────────────── */

/** Shape of error responses from the backend and */
export interface ApiErrorResponse {
  message: string;
  statusCode: number;
  timestamp: string;
}
