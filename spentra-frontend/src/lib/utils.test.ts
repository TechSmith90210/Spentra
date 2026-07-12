import { describe, it, expect } from 'vitest';
import {
  formatCurrency,
  formatInputAmount,
  formatDate,
  formatMonth,
  getCurrentMonth,
  cn,
  getInitials,
  getCategoryIcon,
} from './utils';

describe('utils.ts', () => {
  describe('formatCurrency', () => {
    it('formats numbers as INR by default', () => {
      const formatted = formatCurrency(123456.78);
      expect(formatted).toContain('1,23,456.78');
    });

    it('formats numbers as USD', () => {
      const formatted = formatCurrency(123456.78, 'USD');
      expect(formatted).toContain('123,456.78');
    });
  });

  describe('formatInputAmount', () => {
    it('formats input while typing for INR', () => {
      expect(formatInputAmount('123456')).toBe('1,23,456');
      expect(formatInputAmount('-123456')).toBe('-1,23,456');
      expect(formatInputAmount('1234.5')).toBe('1,234.5');
      expect(formatInputAmount('')).toBe('');
      expect(formatInputAmount('-')).toBe('-');
    });

    it('formats input while typing for USD', () => {
      expect(formatInputAmount('123456', 'USD')).toBe('123,456');
      expect(formatInputAmount('-123456', 'USD')).toBe('-123,456');
    });
  });

  describe('formatDate', () => {
    it('formats ISO dates correctly', () => {
      const result = formatDate('2026-06-27T00:00:00');
      expect(result).toBe('Jun 27, 2026');
    });
  });

  describe('formatMonth', () => {
    it('formats month strings correctly', () => {
      expect(formatMonth('2026-06')).toBe('June 2026');
      expect(formatMonth('2026-12')).toBe('December 2026');
    });
  });

  describe('getCurrentMonth', () => {
    it('returns the current month in YYYY-MM format', () => {
      const result = getCurrentMonth();
      expect(result).toMatch(/^\d{4}-\d{2}$/);

      const now = new Date();
      const expected = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
      expect(result).toBe(expected);
    });
  });

  describe('cn', () => {
    it('concatenates truthy classes and ignores falsy ones', () => {
      expect(cn('class1', 'class2')).toBe('class1 class2');
      expect(cn('class1', undefined, 'class2', false, null, 'class3')).toBe('class1 class2 class3');
      expect(cn()).toBe('');
    });
  });

  describe('getInitials', () => {
    it('extracts initials correctly', () => {
      expect(getInitials('John Doe')).toBe('JD');
      expect(getInitials('John')).toBe('J');
      expect(getInitials('john doe')).toBe('JD');
      expect(getInitials('John Middlename Doe')).toBe('JM');
      expect(getInitials('')).toBe('');
      expect(getInitials('   ')).toBe('');
    });
  });

  describe('getCategoryIcon', () => {
    it('returns the correct icon for exact matches', () => {
      expect(getCategoryIcon('food')).toBe('utensils');
      expect(getCategoryIcon('transport')).toBe('car');
      expect(getCategoryIcon('education')).toBe('graduation-cap');
      expect(getCategoryIcon('gaming')).toBe('gamepad-2');
      expect(getCategoryIcon('salary')).toBe('banknote');
    });

    it('normalizes casing and returns the correct icon', () => {
      expect(getCategoryIcon('Food')).toBe('utensils');
      expect(getCategoryIcon('TRANSPORT')).toBe('car');
      expect(getCategoryIcon('EdUcaTiOn')).toBe('graduation-cap');
    });

    it('normalizes whitespaces and returns the correct icon', () => {
      expect(getCategoryIcon('  food  ')).toBe('utensils');
      expect(getCategoryIcon('transport ')).toBe('car');
      expect(getCategoryIcon(' education')).toBe('graduation-cap');
    });

    it('handles combined casing and whitespaces', () => {
      expect(getCategoryIcon('  FoOd  ')).toBe('utensils');
    });

    it('falls back to "circle-dot" for unknown categories', () => {
      expect(getCategoryIcon('unknown-category')).toBe('circle-dot');
      expect(getCategoryIcon('something random')).toBe('circle-dot');
    });

    it('falls back to "circle-dot" for empty/whitespace-only input', () => {
      expect(getCategoryIcon('')).toBe('circle-dot');
      expect(getCategoryIcon('   ')).toBe('circle-dot');
    });
  });
});

describe('formatInputAmount', () => {
  describe('basic formatting', () => {
    it('returns empty string for empty input', () => {
      expect(formatInputAmount('')).toBe('');
    });

    it('formats positive integers with default INR locale (Indian numbering system)', () => {
      expect(formatInputAmount('123')).toBe('123');
      expect(formatInputAmount('1234')).toBe('1,234');
      expect(formatInputAmount('12345')).toBe('12,345');
      expect(formatInputAmount('123456')).toBe('1,23,456');
      expect(formatInputAmount('1234567')).toBe('12,34,567');
    });

    it('formats positive integers with USD locale (US numbering system)', () => {
      expect(formatInputAmount('123', 'USD')).toBe('123');
      expect(formatInputAmount('1234', 'USD')).toBe('1,234');
      expect(formatInputAmount('1234567', 'USD')).toBe('1,234,567');
    });

    it('formats negative integers correctly', () => {
      expect(formatInputAmount('-123')).toBe('-123');
      expect(formatInputAmount('-1234')).toBe('-1,234');
      expect(formatInputAmount('-1234567', 'USD')).toBe('-1,234,567');
    });
  });

  describe('decimals and fraction formatting', () => {
    it('formats positive decimals correctly', () => {
      expect(formatInputAmount('1234.56')).toBe('1,234.56');
      expect(formatInputAmount('1234567.89', 'USD')).toBe('1,234,567.89');
    });

    it('formats negative decimals correctly', () => {
      expect(formatInputAmount('-1234.56')).toBe('-1,234.56');
      expect(formatInputAmount('-1234567.89', 'USD')).toBe('-1,234,567.89');
    });
  });

  describe('active typing inputs and edge cases', () => {
    it('handles a single minus sign correctly', () => {
      expect(formatInputAmount('-')).toBe('-');
    });

    it('handles a single decimal point correctly', () => {
      expect(formatInputAmount('.')).toBe('.');
    });

    it('handles a trailing decimal point correctly', () => {
      expect(formatInputAmount('1234.')).toBe('1,234.');
      expect(formatInputAmount('-1234.')).toBe('-1,234.');
    });

    it('handles minus sign and decimal point ("-.") correctly', () => {
      expect(formatInputAmount('-.')).toBe('-.');
    });

    it('handles negative decimals starting with dot ("-.5") correctly', () => {
      expect(formatInputAmount('-.5')).toBe('-.5');
    });

    it('handles multiple leading zeroes correctly', () => {
      expect(formatInputAmount('000123')).toBe('123');
      expect(formatInputAmount('0')).toBe('0');
      expect(formatInputAmount('000')).toBe('0');
      expect(formatInputAmount('-000123')).toBe('-123');
    });

    it('handles multiple decimal points gracefully', () => {
      expect(formatInputAmount('1.2.3')).toBe('1.2');
    });
  });
});
