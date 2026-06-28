/**
 * @file ProgressBar/index.tsx
 * @description Spentra design system ProgressBar component.
 *
 * A horizontal bar indicator for budget usage and goal progress.
 * Supports auto-variant mode that shifts color based on value thresholds:
 * >80% = danger, >60% = warning, otherwise default.
 *
 * @example
 * ```tsx
 * // Auto-variant based on value
 * <ProgressBar value={75} label="Monthly Budget" showPercentage />
 *
 * // Explicit variant
 * <ProgressBar value={30} variant="success" />
 * ```
 */

'use client';

/** Color variant for the progress fill */
type ProgressVariant = 'default' | 'success' | 'warning' | 'danger';

/** Size preset */
type ProgressSize = 'sm' | 'md';

/** Props for the ProgressBar component */
export interface ProgressBarProps {
  /** Progress value from 0 to 100 */
  value: number;
  /** Color variant — if omitted, auto-selects based on value thresholds */
  variant?: ProgressVariant;
  /** Label text displayed above the bar */
  label?: string;
  /** Whether to show the percentage number */
  showPercentage?: boolean;
  /** Track height preset */
  size?: ProgressSize;
  /** Additional CSS classes */
  className?: string;
}

/** Tailwind fill color classes for each variant */
const fillColors: Record<ProgressVariant, string> = {
  default: 'bg-primary',
  success: 'bg-green-500',
  warning: 'bg-amber-500',
  danger: 'bg-error',
};

/** Height classes for each size */
const sizeStyles: Record<ProgressSize, string> = {
  sm: 'h-1.5',
  md: 'h-2.5',
};

/**
 * Determines the auto-variant based on the progress value.
 */
function getAutoVariant(value: number): ProgressVariant {
  if (value > 80) return 'danger';
  if (value > 60) return 'warning';
  return 'default';
}

/**
 * ProgressBar — a horizontal progress indicator.
 *
 * Used for budget utilization, savings goals, and other percentage-based
 * metrics. Includes auto-coloring that shifts from default → warning → danger
 * as the value increases.
 */
export default function ProgressBar({
  value,
  variant,
  label,
  showPercentage = false,
  size = 'sm',
  className = '',
}: ProgressBarProps) {
  /** Clamp value to 0–100 range */
  const clampedValue = Math.min(100, Math.max(0, value));

  /** Resolve the effective variant */
  const effectiveVariant = variant ?? getAutoVariant(clampedValue);

  return (
    <div className={['w-full', className].filter(Boolean).join(' ')}>
      {/* Header with label and percentage */}
      {(label || showPercentage) && (
        <div className="flex items-center justify-between mb-2">
          {label && (
            <span className="text-xs uppercase tracking-widest text-on-surface-variant font-medium">
              {label}
            </span>
          )}
          {showPercentage && (
            <span className="text-xs font-bold text-on-surface tabular-nums">
              {Math.round(clampedValue)}%
            </span>
          )}
        </div>
      )}

      {/* Track */}
      <div
        className={[
          'bg-surface-container-lowest dark:bg-surface-container',
          'rounded-full overflow-hidden',
          sizeStyles[size],
        ].join(' ')}
        role="progressbar"
        aria-valuenow={clampedValue}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label}
      >
        {/* Fill */}
        <div
          className={[
            'h-full rounded-full',
            'transition-all duration-500 ease-out',
            fillColors[effectiveVariant],
          ].join(' ')}
          style={{ width: `${clampedValue}%` }}
        />
      </div>
    </div>
  );
}
