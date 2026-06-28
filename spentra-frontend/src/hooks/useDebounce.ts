/**
 * @file useDebounce.ts
 * @description Hook that debounces a rapidly-changing value.
 *
 * Useful for search inputs, filter controls, and any UI where you want
 * to wait until the user stops typing before triggering an expensive
 * operation (e.g. an API call).
 *
 * @example
 * ```tsx
 * const [search, setSearch] = useState('');
 * const debouncedSearch = useDebounce(search, 300);
 *
 * useEffect(() => {
 *   // Only fires 300ms after the user stops typing
 *   fetchResults(debouncedSearch);
 * }, [debouncedSearch]);
 * ```
 */

'use client';

import { useEffect, useState } from 'react';

/**
 * Returns a debounced version of the given value.
 *
 * @typeParam T - Type of the value being debounced
 * @param value - The rapidly-changing input value
 * @param delay - Debounce delay in milliseconds
 * @returns The debounced value (updates only after `delay` ms of inactivity)
 */
export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}
