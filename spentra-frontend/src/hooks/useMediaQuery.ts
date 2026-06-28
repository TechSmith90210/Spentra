/**
 * @file useMediaQuery.ts
 * @description Reactive hook that tracks whether a CSS media query matches.
 *
 * Uses `window.matchMedia` under the hood and listens for changes,
 * returning `false` during SSR to avoid hydration mismatches.
 *
 * @example
 * ```tsx
 * const isMobile = useMediaQuery('(max-width: 768px)');
 * ```
 */

'use client';

import { useEffect, useState } from 'react';

/**
 * Subscribe to a CSS media query and re-render when it changes.
 *
 * @param query - CSS media query string (e.g. `"(min-width: 1024px)"`)
 * @returns `true` when the query matches, `false` otherwise (or during SSR)
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState<boolean>(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia(query);

    // Set initial value
    setMatches(mediaQuery.matches);

    /** Update state when the media query match status changes */
    function handleChange(event: MediaQueryListEvent): void {
      setMatches(event.matches);
    }

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, [query]);

  return matches;
}
