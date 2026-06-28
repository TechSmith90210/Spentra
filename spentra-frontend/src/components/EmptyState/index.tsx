/**
 * @file EmptyState/index.tsx
 * @description Spentra design system EmptyState component.
 *
 * A centered placeholder for screens or sections with no data.
 * Accepts a large icon, title, description, and an action slot
 * for a CTA button.
 *
 * @example
 * ```tsx
 * <EmptyState
 *   icon={<Receipt />}
 *   title="No transactions yet"
 *   description="Add your first expense or income to get started."
 *   action={<Button variant="primary">Add Transaction</Button>}
 * />
 * ```
 */

'use client';

import { type ReactNode } from 'react';

/** Props for the EmptyState component */
export interface EmptyStateProps {
  /** Large icon displayed at the top */
  icon?: ReactNode;
  /** Heading text */
  title: string;
  /** Supporting description text */
  description?: string;
  /** Optional action slot (typically a Button) */
  action?: ReactNode;
}

/**
 * EmptyState — a centered placeholder for empty data states.
 *
 * Uses generous padding and muted icon styling following the
 * editorial precision aesthetic.
 */
export default function EmptyState({
  icon,
  title,
  description,
  action,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      {/* Icon */}
      {icon && (
        <div className="text-on-surface-variant/50 mb-6 [&>svg]:w-16 [&>svg]:h-16">
          {icon}
        </div>
      )}

      {/* Title */}
      <h3 className="text-lg font-bold tracking-tight text-on-surface mb-2">
        {title}
      </h3>

      {/* Description */}
      {description && (
        <p className="text-sm text-on-surface-variant max-w-sm mb-6">
          {description}
        </p>
      )}

      {/* Action slot */}
      {action && <div>{action}</div>}
    </div>
  );
}
