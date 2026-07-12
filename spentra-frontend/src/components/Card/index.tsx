/**
 * @file Card/index.tsx
 * @description Spentra design system Card component.
 *
 * A surface container following the "Monolith & Void" aesthetic —
 * uses background color shifts instead of borders for visual hierarchy.
 * Three variants map to the surface elevation system.
 *
 * @example
 * ```tsx
 * <Card variant="elevated" padding="lg">
 *   <h2>Monthly Summary</h2>
 * </Card>
 *
 * <Card variant="filled" interactive onClick={handleClick}>
 *   <p>Click me</p>
 * </Card>
 * ```
 */

'use client';

import { type HTMLAttributes, type ReactNode } from 'react';

/** Visual variant controlling background and elevation */
type CardVariant = 'elevated' | 'filled' | 'outlined';

/** Padding preset */
type CardPadding = 'sm' | 'md' | 'lg';

/** Props for the Card component */
export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** Background and elevation variant */
  variant?: CardVariant;
  /** Internal padding preset */
  padding?: CardPadding;
  /** Card content */
  children: ReactNode;
  /** Whether the card responds to hover/click interactions */
  interactive?: boolean;
}

/** Tailwind classes for each variant */
const variantStyles: Record<CardVariant, string> = {
  elevated:
    'bg-surface-container-lowest shadow-sm',
  filled:
    'bg-surface-container-low',
  outlined:
    'bg-surface-container-lowest border border-outline-variant/10',
};

/** Tailwind classes for each padding preset */
const paddingStyles: Record<CardPadding, string> = {
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8',
};

/**
 * Card — a surface container component for grouping related content.
 *
 * Follows the Spentra surface hierarchy system with 1.5rem rounded
 * corners and optional interactive hover states.
 */
export default function Card({
  variant = 'elevated',
  padding = 'md',
  children,
  interactive = false,
  className = '',
  onClick,
  ...props
}: CardProps) {
  return (
    <div
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      onClick={onClick}
      onKeyDown={
        interactive
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onClick?.(e as unknown as React.MouseEvent<HTMLDivElement>);
              }
            }
          : undefined
      }
      className={[
        // Base
        'rounded-[1.5rem]',
        // Variant
        variantStyles[variant],
        // Padding
        paddingStyles[padding],
        // Interactive
        interactive
          ? 'hover:shadow-lg hover:-translate-y-1 hover:scale-[1.01] transition-all duration-300 ease-out cursor-pointer active:scale-[0.99] active:translate-y-0'
          : '',
        // Custom overrides
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      {...props}
    >
      {children}
    </div>
  );
}
