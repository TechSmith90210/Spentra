/**
 * @file ThemeToggle/index.tsx
 * @description Spentra design system ThemeToggle component.
 *
 * A compact icon button that toggles between light and dark mode
 * using the ThemeProvider context. Displays Moon in light mode
 * and Sun in dark mode.
 *
 * @example
 * ```tsx
 * <ThemeToggle />
 * ```
 */

'use client';

import { Moon, Sun } from 'lucide-react';
import { useTheme } from '@/providers/ThemeProvider';

/**
 * ThemeToggle — an icon button for switching between light and dark mode.
 *
 * Consumes the `useTheme()` hook from ThemeProvider and renders the
 * appropriate sun/moon icon with a smooth scale transition on click.
 */
export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      className={[
        'p-2 rounded-lg cursor-pointer',
        'hover:bg-surface-container-low',
        'transition-all duration-200 active:scale-95',
        'text-on-surface-variant hover:text-on-surface',
      ].join(' ')}
      aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
      type="button"
    >
      {theme === 'light' ? (
        <Moon className="w-5 h-5 transition-transform duration-300" />
      ) : (
        <Sun className="w-5 h-5 transition-transform duration-300" />
      )}
    </button>
  );
}
