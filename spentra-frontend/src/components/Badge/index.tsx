/**
 * @file Badge/index.tsx
 * @description Spentra design system Badge component.
 *
 * A compact, pill-shaped label for categorizing transactions and statuses.
 * Five semantic variants: income (green), expense (red), neutral (grey),
 * warning (amber), and recurring (indigo/tertiary).
 *
 * @example
 * ```tsx
 * <Badge variant="income">Income</Badge>
 * <Badge variant="expense" size="sm">-$42.00</Badge>
 * <Badge variant="recurring">Monthly</Badge>
 * ```
 */

'use client';

import { type ReactNode } from 'react';

/** Semantic color variant */
type BadgeVariant = 'income' | 'expense' | 'neutral' | 'warning' | 'recurring';

/** Size preset */
type BadgeSize = 'sm' | 'md';

/** Props for the Badge component */
export interface BadgeProps {
  /** Semantic color variant */
  variant?: BadgeVariant;
  /** Badge content */
  children: ReactNode;
  /** Size preset */
  size?: BadgeSize;
  /** Additional CSS classes */
  className?: string;
}

/** Tailwind classes for each variant */
const variantStyles: Record<BadgeVariant, string> = {
  income: 'bg-secondary-container/30 text-on-secondary-container',
  expense: 'bg-error-container/20 text-on-error-container dark:text-error',
  neutral: 'bg-surface-container-high text-on-surface-variant',
  warning: 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400',
  recurring: 'bg-tertiary/10 text-tertiary',
};

/** Tailwind classes for each size */
const sizeStyles: Record<BadgeSize, string> = {
  sm: 'px-2 py-0.5 text-[10px]',
  md: 'px-3 py-1 text-xs',
};

/**
 * Badge — a compact pill label for transaction types and statuses.
 *
 * Uses muted container backgrounds with semantic text colors,
 * uppercase tracking for the editorial precision aesthetic.
 */
export default function Badge({
  variant = 'neutral',
  children,
  size = 'md',
  className = '',
}: BadgeProps) {
  return (
    <span
      className={[
        'inline-flex items-center',
        'rounded-full font-bold uppercase tracking-wider',
        variantStyles[variant],
        sizeStyles[size],
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {children}
    </span>
  );
}
