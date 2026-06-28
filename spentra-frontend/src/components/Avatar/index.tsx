/**
 * @file Avatar/index.tsx
 * @description Spentra design system Avatar component.
 *
 * Displays a user's profile image or a fallback with their initials
 * on a primary-container background. Supports three size presets.
 *
 * @example
 * ```tsx
 * // With image
 * <Avatar name="Salman Khan" src="/avatar.jpg" size="lg" />
 *
 * // Initials fallback
 * <Avatar name="Salman Khan" />
 * ```
 */

'use client';

/** Size preset */
type AvatarSize = 'sm' | 'md' | 'lg';

/** Props for the Avatar component */
export interface AvatarProps {
  /** User's full name — used for alt text and initials fallback */
  name: string;
  /** Size preset */
  size?: AvatarSize;
  /** Optional profile image URL */
  src?: string;
  /** Additional CSS classes */
  className?: string;
}

/** Tailwind classes for each size */
const sizeStyles: Record<AvatarSize, string> = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-12 h-12 text-base',
};

/**
 * Extracts up to two initials from a full name string.
 *
 * @example
 * getInitials('Salman Khan') // 'SK'
 * getInitials('Salman')      // 'S'
 */
function getInitials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

/**
 * Avatar — a circular user identifier component.
 *
 * Renders the user's profile image when available, or falls back
 * to their initials on a primary-container background.
 */
export default function Avatar({
  name,
  size = 'md',
  src,
  className = '',
}: AvatarProps) {
  const initials = getInitials(name);

  const baseClasses = [
    'rounded-full shrink-0',
    'inline-flex items-center justify-center',
    'font-bold select-none',
    sizeStyles[size],
    className,
  ]
    .filter(Boolean)
    .join(' ');

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={[baseClasses, 'object-cover'].join(' ')}
      />
    );
  }

  return (
    <div
      className={[baseClasses, 'bg-primary-container text-on-primary-container'].join(
        ' '
      )}
      aria-label={name}
      role="img"
    >
      {initials}
    </div>
  );
}
