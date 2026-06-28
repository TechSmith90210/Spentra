/**
 * @file Skeleton/index.tsx
 * @description Spentra design system Skeleton loading placeholder.
 *
 * Provides text, circular, and rectangular skeleton variants for
 * content loading states. Uses animate-pulse with surface-container
 * backgrounds.
 *
 * @example
 * ```tsx
 * // Single text skeleton
 * <Skeleton variant="text" width="60%" />
 *
 * // Multi-line paragraph skeleton
 * <Skeleton variant="text" count={3} />
 *
 * // Avatar placeholder
 * <Skeleton variant="circular" width={40} height={40} />
 *
 * // Card placeholder
 * <Skeleton variant="rectangular" height={120} />
 * ```
 */

'use client';

/** Visual shape variant */
type SkeletonVariant = 'text' | 'circular' | 'rectangular';

/** Props for the Skeleton component */
export interface SkeletonProps {
  /** Additional CSS classes */
  className?: string;
  /** Shape variant */
  variant?: SkeletonVariant;
  /** Width — accepts CSS values (string) or pixel numbers */
  width?: string | number;
  /** Height — accepts CSS values (string) or pixel numbers */
  height?: string | number;
  /** Number of skeleton lines (only applies to text variant) */
  count?: number;
}

/** Border radius classes for each variant */
const variantRadius: Record<SkeletonVariant, string> = {
  text: 'rounded',
  circular: 'rounded-full',
  rectangular: 'rounded-xl',
};

/**
 * Converts a width/height value to an inline CSS string.
 */
function toCssValue(value: string | number | undefined): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === 'number' ? `${value}px` : value;
}

/** Staggered widths for multi-line text skeletons */
const LINE_WIDTHS = ['100%', '92%', '78%', '85%', '65%'];

/**
 * Skeleton — a pulsing placeholder for loading states.
 *
 * Matches the Spentra surface hierarchy and provides three shape
 * variants: text (lines), circular (avatars), and rectangular (cards).
 */
export default function Skeleton({
  className = '',
  variant = 'text',
  width,
  height,
  count = 1,
}: SkeletonProps) {
  const baseClasses = [
    'bg-surface-container-high',
    'animate-pulse',
    variantRadius[variant],
    className,
  ]
    .filter(Boolean)
    .join(' ');

  // Text variant with multiple lines
  if (variant === 'text' && count > 1) {
    return (
      <div className="space-y-2">
        {Array.from({ length: count }, (_, i) => (
          <div
            key={i}
            className={baseClasses}
            style={{
              width: width ? toCssValue(width) : LINE_WIDTHS[i % LINE_WIDTHS.length],
              height: toCssValue(height) ?? '1rem',
            }}
          />
        ))}
      </div>
    );
  }

  // Single skeleton element
  return (
    <div
      className={baseClasses}
      style={{
        width: toCssValue(width) ?? (variant === 'circular' ? '2.5rem' : '100%'),
        height:
          toCssValue(height) ??
          (variant === 'text'
            ? '1rem'
            : variant === 'circular'
              ? toCssValue(width) ?? '2.5rem'
              : '5rem'),
      }}
    />
  );
}
