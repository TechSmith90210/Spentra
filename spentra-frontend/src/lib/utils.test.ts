import { describe, it, expect } from 'vitest';
import { formatInputAmount } from './utils';

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
