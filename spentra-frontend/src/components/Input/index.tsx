/**
 * @file Input/index.tsx
 * @description Spentra design system Input component.
 *
 * A styled text input with editorial label formatting (uppercase, tracked),
 * optional leading icon, and validation error display. Uses surface-container
 * backgrounds with ring-based focus indicators.
 *
 * @example
 * ```tsx
 * <Input
 *   label="Amount"
 *   type="number"
 *   placeholder="0.00"
 *   icon={<DollarSign />}
 *   error="Amount is required"
 * />
 * ```
 */

'use client';

import { type InputHTMLAttributes, type ReactNode, useId } from 'react';

/** Props for the Input component */
export interface InputProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  /** Label text displayed above the input */
  label?: string;
  /** Validation error message */
  error?: string;
  /** Optional icon rendered on the left side of the input */
  icon?: ReactNode;
  /** HTML id attribute — auto-generated if not provided */
  id?: string;
}

/**
 * Input — a form text input with editorial-precision styling.
 *
 * Features an uppercase tracked label, surface-container background,
 * tertiary ring focus state, and error messaging.
 */
export default function Input({
  label,
  error,
  icon,
  disabled,
  className = '',
  id: externalId,
  ...props
}: InputProps) {
  const generatedId = useId();
  const inputId = externalId ?? generatedId;

  return (
    <div className={['w-full', className].filter(Boolean).join(' ')}>
      {/* Editorial label */}
      {label && (
        <label
          htmlFor={inputId}
          className="block text-xs uppercase tracking-widest text-on-surface-variant mb-2 font-medium"
        >
          {label}
        </label>
      )}

      {/* Input wrapper for icon positioning */}
      <div className="relative">
        {/* Leading icon */}
        {icon && (
          <span
            className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant [&>svg]:w-4 [&>svg]:h-4"
            aria-hidden="true"
          >
            {icon}
          </span>
        )}

        <input
          id={inputId}
          disabled={disabled}
          className={[
            // Base
            'w-full bg-surface-container-low border-none rounded-xl',
            'px-4 py-3 text-on-surface text-sm',
            'placeholder:text-on-surface-variant/50',
            'outline-none transition-all duration-200',
            // Focus
            error
              ? 'ring-1 ring-error focus:ring-2 focus:ring-error'
              : 'focus:bg-surface-container-lowest focus:ring-1 focus:ring-tertiary',
            // Icon offset
            icon ? 'pl-10' : '',
            // Disabled
            disabled ? 'opacity-50 cursor-not-allowed' : '',
          ]
            .filter(Boolean)
            .join(' ')}
          {...props}
        />
      </div>

      {/* Error message */}
      {error && (
        <p className="text-error text-xs mt-1 font-medium" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
