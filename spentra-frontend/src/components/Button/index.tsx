/**
 * @file Button/index.tsx
 * @description Spentra design system Button component.
 *
 * Implements the "Editorial Precision" aesthetic with void-gradient primary
 * buttons, surface-hierarchy secondary buttons, transparent tertiary links,
 * and high-contrast danger actions.
 *
 * @example
 * ```tsx
 * <Button variant="primary" icon={<Plus />}>New Entry</Button>
 * <Button variant="secondary" size="sm">Cancel</Button>
 * <Button variant="danger" loading>Deleting…</Button>
 * ```
 */

'use client';

import { type ButtonHTMLAttributes, type ReactNode } from 'react';

/** Visual variant of the button */
type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'danger';

/** Size preset controlling padding and font size */
type ButtonSize = 'sm' | 'md' | 'lg';

/** Props for the Button component */
export interface ButtonProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  /** Visual style variant */
  variant?: ButtonVariant;
  /** Size preset */
  size?: ButtonSize;
  /** Button content */
  children: ReactNode;
  /** Whether the button is in a loading state */
  loading?: boolean;
  /** Stretch to fill container width */
  fullWidth?: boolean;
  /** HTML button type attribute */
  type?: 'button' | 'submit' | 'reset';
  /** Optional icon rendered before children */
  icon?: ReactNode;
}

/** Tailwind classes for each variant */
const variantStyles: Record<ButtonVariant, string> = {
  primary:
    'void-gradient text-on-primary shadow-sm hover:shadow-md hover:-translate-y-0.5 hover:brightness-105 active:translate-y-0',
  secondary:
    'bg-surface-container-high text-on-surface hover:bg-surface-container-highest hover:-translate-y-0.5 active:translate-y-0',
  tertiary:
    'bg-transparent text-tertiary hover:underline underline-offset-4',
  danger:
    'bg-error text-on-error hover:bg-error/90 hover:-translate-y-0.5 active:translate-y-0',
};

/** Tailwind classes for each size */
const sizeStyles: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-5 py-2.5 text-sm',
  lg: 'px-6 py-3 text-base',
};

/**
 * Button — the primary action element for the Spentra UI.
 *
 * Supports four visual variants, three sizes, loading state with
 * an animated spinner, and optional leading icon.
 */
export default function Button({
  variant = 'primary',
  size = 'md',
  children,
  loading = false,
  fullWidth = false,
  type = 'button',
  icon,
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      type={type}
      disabled={isDisabled}
      className={[
        // Base
        'inline-flex items-center justify-center gap-2',
        'rounded-xl font-semibold tracking-tight',
        'transition-all duration-200 active:scale-95',
        'cursor-pointer select-none',
        // Variant
        variantStyles[variant],
        // Size
        sizeStyles[size],
        // Full width
        fullWidth ? 'w-full' : '',
        // Disabled
        isDisabled ? 'opacity-50 pointer-events-none' : '',
        // Custom overrides
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      {...props}
    >
      {/* Loading spinner */}
      {loading && (
        <svg
          className="animate-spin h-4 w-4 shrink-0"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
      )}

      {/* Leading icon (hidden when loading) */}
      {!loading && icon && (
        <span className="shrink-0 [&>svg]:w-4 [&>svg]:h-4">{icon}</span>
      )}

      {children}
    </button>
  );
}
