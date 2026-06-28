/**
 * @file Settings provider and hook.
 *
 * Manages user preferences like currency code, persisted to localStorage.
 */

'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

export type CurrencyCode = 'INR' | 'USD' | 'EUR' | 'GBP';

interface SettingsContextValue {
  currency: CurrencyCode;
  setCurrency: (currency: CurrencyCode) => void;
}

const SettingsContext = createContext<SettingsContextValue | null>(null);

const CURRENCY_KEY = 'spentra_currency';

export function SettingsProvider({ children }: { children: React.ReactNode }) {
  const [currency, setCurrencyState] = useState<CurrencyCode>('INR');

  useEffect(() => {
    const stored = localStorage.getItem(CURRENCY_KEY) as CurrencyCode | null;
    if (stored) {
      setCurrencyState(stored);
    }
  }, []);

  const setCurrency = useCallback((newCurrency: CurrencyCode) => {
    localStorage.setItem(CURRENCY_KEY, newCurrency);
    setCurrencyState(newCurrency);
  }, []);

  return (
    <SettingsContext.Provider value={{ currency, setCurrency }}>
      {children}
    </SettingsContext.Provider>
  );
}

/**
 * Access settings state from any client component.
 */
export function useSettings(): SettingsContextValue {
  const context = useContext(SettingsContext);
  if (!context) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
}
