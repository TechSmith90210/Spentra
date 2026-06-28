/**
 * @file Select/index.tsx
 * @description Spentra design system Select component.
 *
 * A styled select dropdown matching the Input component's aesthetic.
 * Uses appearance-none with a custom ChevronDown indicator from lucide-react.
 *
 * @example
 * ```tsx
 * <Select
 *   label="Category"
 *   placeholder="Choose a category"
 *   options={[
 *     { value: 'food', label: 'Food & Dining' },
 *     { value: 'transport', label: 'Transportation' },
 *   ]}
 *   value={category}
 *   onChange={(e) => setCategory(e.target.value)}
 * />
 * ```
 */

'use client';

import { type SelectHTMLAttributes, useId } from 'react';
import { ChevronDown } from 'lucide-react';

/** A single option in the select dropdown */
export interface SelectOption {
  /** Option value */
  value: string;
  /** Display label */
  label: string;
}

/** Props for the Select component */
export interface SelectProps
  extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> {
  /** Label text displayed above the select */
  label?: string;
  /** Validation error message */
  error?: string;
  /** Available options */
  options: SelectOption[];
  /** Placeholder text shown when no value is selected */
  placeholder?: string;
  /** HTML id attribute — auto-generated if not provided */
  id?: string;
}

/**
 * Select — a styled dropdown matching the Spentra editorial input aesthetic.
 *
 * Features appearance-none styling, a custom chevron indicator,
 * and the same label/error patterns as the Input component.
 */
export default function Select({
  label,
  error,
  options,
  placeholder,
  disabled,
  className = '',
  id: externalId,
  ...props
}: SelectProps) {
  const generatedId = useId();
  const selectId = externalId ?? generatedId;

  return (
    <div className={['w-full', className].filter(Boolean).join(' ')}>
      {/* Editorial label */}
      {label && (
        <label
          htmlFor={selectId}
          className="block text-xs uppercase tracking-widest text-on-surface-variant mb-2 font-medium"
        >
          {label}
        </label>
      )}

      {/* Select wrapper for custom chevron positioning */}
      <div className="relative">
        <select
          id={selectId}
          disabled={disabled}
          className={[
            // Base — matches Input styling
            'w-full appearance-none bg-surface-container-low border-none rounded-xl',
            'px-4 py-3 pr-10 text-on-surface text-sm',
            'outline-none transition-all duration-200 cursor-pointer',
            // Focus
            error
              ? 'ring-1 ring-error focus:ring-2 focus:ring-error'
              : 'focus:bg-surface-container-lowest focus:ring-1 focus:ring-tertiary',
            // Disabled
            disabled ? 'opacity-50 cursor-not-allowed' : '',
          ]
            .filter(Boolean)
            .join(' ')}
          {...props}
        >
          {/* Placeholder option */}
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}

          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        {/* Custom chevron indicator */}
        <ChevronDown
          className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant pointer-events-none"
          aria-hidden="true"
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
