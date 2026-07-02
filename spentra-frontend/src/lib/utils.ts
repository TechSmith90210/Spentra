/**
 * @file utils.ts
 * @description Shared utility functions used across the Spentra frontend.
 *
 * Includes formatting helpers, a classname merger, and a category-to-icon mapper.
 */

/* ─── Currency Formatting ───────────────────────────────────────────────────── */

/**
 * Format a number as USD currency string.
 *
 * @param amount - Raw numeric amount
 * @returns Formatted string like `$1,234.56`
 *
 * @example
 * ```ts
 * formatCurrency(1234.5) // "$1,234.50"
 * formatCurrency(0)      // "$0.00"
 * ```
 */
export function formatCurrency(amount: number, currencyCode: string = 'INR'): string {
  const locale = currencyCode === 'INR' ? 'en-IN' : 'en-US';
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currencyCode,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

/**
 * Formats a raw numeric string into a comma-separated string while typing.
 * Supports negative numbers and decimals.
 */
export function formatInputAmount(value: string, currencyCode: string = 'INR'): string {
  if (!value) return '';
  const isNegative = value.startsWith('-');
  const rawValue = isNegative ? value.slice(1) : value;
  
  const [int, dec] = rawValue.split('.');
  if (!int && isNegative) return '-';
  
  const parsedInt = parseInt(int, 10);
  const locale = currencyCode === 'INR' ? 'en-IN' : 'en-US';
  let formattedInt = isNaN(parsedInt) ? '' : new Intl.NumberFormat(locale).format(parsedInt);
  
  if (isNegative && formattedInt) formattedInt = '-' + formattedInt;
  
  if (value.includes('.')) {
    return `${formattedInt}.${dec || ''}`;
  }
  return formattedInt || (isNegative ? '-' : '');
}

/* ─── Date Formatting ───────────────────────────────────────────────────────── */

/**
 * Format an ISO date string into a human-readable date.
 *
 * @param dateStr - ISO-8601 date string (e.g. `"2026-06-27T00:00:00"`)
 * @returns Formatted string like `"Jun 27, 2026"`
 */
export function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date);
}

/**
 * Format a `YYYY-MM` month string into a human-readable month label.
 *
 * @param monthStr - Month string like `"2026-06"`
 * @returns Formatted string like `"June 2026"`
 */
export function formatMonth(monthStr: string): string {
  const [year, month] = monthStr.split('-');
  const date = new Date(Number(year), Number(month) - 1, 1);
  return new Intl.DateTimeFormat('en-US', {
    month: 'long',
    year: 'numeric',
  }).format(date);
}

/**
 * Get the current month in `YYYY-MM` format.
 *
 * @returns String like `"2026-06"`
 */
export function getCurrentMonth(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

/* ─── Classname Utility ─────────────────────────────────────────────────────── */

/**
 * Merge CSS class names, filtering out falsy values.
 * Lightweight alternative to `clsx` / `classnames` — no deduplication,
 * just concatenation with falsy-filtering.
 *
 * @param classes - Variable list of class strings or falsy values
 * @returns Merged class string
 *
 * @example
 * ```ts
 * cn('px-4', isActive && 'bg-primary', undefined, 'text-sm')
 * // → "px-4 bg-primary text-sm"
 * ```
 */
export function cn(
  ...classes: (string | undefined | false | null)[]
): string {
  return classes.filter(Boolean).join(' ');
}

/* ─── String Helpers ────────────────────────────────────────────────────────── */

/**
 * Extract up to two initials from a full name.
 *
 * @param name - Full name string
 * @returns Uppercase initials (e.g. `"JD"` for `"John Doe"`)
 */
export function getInitials(name: string): string {
  if (!name) return '';
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

/* ─── Category Icons ────────────────────────────────────────────────────────── */

/**
 * Map a category name to a Lucide icon name.
 * Falls back to `"circle-dot"` for unknown categories.
 *
 * @param categoryName - Name of the spending category
 * @returns Lucide icon name string (kebab-case)
 */
export function getCategoryIcon(categoryName: string): string {
  const normalized = categoryName.toLowerCase().trim();

  const iconMap: Record<string, string> = {
    food: 'utensils',
    dining: 'utensils',
    restaurant: 'utensils',
    restaurants: 'utensils',
    groceries: 'shopping-cart',
    grocery: 'shopping-cart',
    transport: 'car',
    transportation: 'car',
    travel: 'plane',
    flights: 'plane',
    entertainment: 'film',
    movies: 'film',
    gaming: 'gamepad-2',
    shopping: 'shopping-bag',
    clothing: 'shirt',
    health: 'heart-pulse',
    healthcare: 'heart-pulse',
    medical: 'heart-pulse',
    fitness: 'dumbbell',
    gym: 'dumbbell',
    education: 'graduation-cap',
    books: 'book-open',
    utilities: 'zap',
    electricity: 'zap',
    water: 'droplets',
    internet: 'wifi',
    phone: 'smartphone',
    rent: 'home',
    housing: 'home',
    mortgage: 'home',
    insurance: 'shield',
    salary: 'banknote',
    income: 'banknote',
    freelance: 'laptop',
    investments: 'trending-up',
    savings: 'piggy-bank',
    gifts: 'gift',
    donations: 'hand-heart',
    subscriptions: 'repeat',
    pets: 'paw-print',
    personal: 'user',
    miscellaneous: 'circle-dot',
    other: 'circle-dot',
  };

  return iconMap[normalized] || 'circle-dot';
}
