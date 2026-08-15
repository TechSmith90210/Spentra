import { describe, it, expect } from 'vitest';
import type { Transaction } from '@/lib/api/types';

function computeMonthlySpendingTrends(transactions: Transaction[]) {
  const spendingMap = new Map<string, number>();

  transactions.forEach((t) => {
    if (t.type === 'CREDIT' || !t.transactionDate || typeof t.amount !== 'number' || t.amount <= 0) return;
    const dateStr = t.transactionDate.split('T')[0].split(' ')[0];
    const parts = dateStr.split('-');
    if (parts.length >= 2) {
      const y = parseInt(parts[0], 10);
      const m = parseInt(parts[1], 10);
      if (!isNaN(y) && !isNaN(m) && m >= 1 && m <= 12) {
        const monthKey = `${y}-${String(m).padStart(2, '0')}`;
        spendingMap.set(monthKey, (spendingMap.get(monthKey) || 0) + t.amount);
      }
    }
  });

  const monthKeysWithTx = Array.from(spendingMap.keys()).sort();
  if (monthKeysWithTx.length === 0) return { items: [] };

  const firstParts = monthKeysWithTx[0].split('-').map(Number);
  const lastParts = monthKeysWithTx[monthKeysWithTx.length - 1].split('-').map(Number);

  const startYear = firstParts[0];
  const startMonth = firstParts[1];
  const endYear = lastParts[0];
  const endMonth = lastParts[1];

  const items = [];
  let curY = startYear;
  let curM = startMonth;

  while (curY < endYear || (curY === endYear && curM <= endMonth)) {
    const monthKey = `${curY}-${String(curM).padStart(2, '0')}`;
    const spent = spendingMap.get(monthKey) || 0;
    const dateObj = new Date(curY, curM - 1, 1);
    const shortMonth = dateObj.toLocaleDateString('en-US', { month: 'short' }).toUpperCase();
    const fullLabel = dateObj.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

    items.push({
      year: curY,
      monthIndex: curM - 1,
      monthKey,
      label: shortMonth,
      fullLabel,
      spent: Number(spent.toFixed(2)),
    });

    curM++;
    if (curM > 12) {
      curM = 1;
      curY++;
    }
  }

  return { items };
}

describe('Dashboard Spending Trends Calculation', () => {
  it('correctly aggregates June, July, and August 2026 transactions', () => {
    const mockTransactions: Transaction[] = [
      { id: '1', title: 'Rapido', amount: 43, type: 'EXPENSE', transactionDate: '2026-06-28' },
      { id: '2', title: 'railway ticks', amount: 20, type: 'EXPENSE', transactionDate: '2026-06-28' },
      { id: '3', title: 'haircut', amount: 70, type: 'EXPENSE', transactionDate: '2026-06-28' },
      { id: '4', title: 'New estate', amount: 15000, type: 'EXPENSE', transactionDate: '2026-07-02' },
      { id: '5', title: 'Salary', amount: 35000, type: 'CREDIT', transactionDate: '2026-07-02' },
      { id: '6', title: 'Toothbrush and floss', amount: 316, type: 'EXPENSE', transactionDate: '2026-07-03' },
      { id: '7', title: 'Pani Puri', amount: 80, type: 'EXPENSE', transactionDate: '2026-07-04' },
      { id: '8', title: 'Some chips', amount: 236, type: 'EXPENSE', transactionDate: '2026-07-06' },
      { id: '9', title: 'Croffles', amount: 219, type: 'EXPENSE', transactionDate: '2026-07-07' },
      { id: '10', title: 'Misc', amount: 149, type: 'EXPENSE', transactionDate: '2026-07-09' },
      { id: '11', title: 'Netflix', amount: 499, type: 'EXPENSE', transactionDate: '2026-07-09' },
      { id: '12', title: 'shampoo', amount: 682, type: 'EXPENSE', transactionDate: '2026-07-11' },
      { id: '13', title: 'RE7', amount: 1259, type: 'EXPENSE', transactionDate: '2026-07-12' },
      { id: '14', title: 'Waffles', amount: 222, type: 'EXPENSE', transactionDate: '2026-07-12' },
      { id: '15', title: 'Haircut', amount: 700, type: 'EXPENSE', transactionDate: '2026-07-14' },
      { id: '16', title: 'Scooter', amount: 1500, type: 'EXPENSE', transactionDate: '2026-07-17' },
      { id: '17', title: 'Momos', amount: 60, type: 'EXPENSE', transactionDate: '2026-07-19' },
      { id: '18', title: 'Idli', amount: 79.99, type: 'EXPENSE', transactionDate: '2026-07-21' },
      { id: '19', title: 'VI Plan', amount: 365, type: 'EXPENSE', transactionDate: '2026-07-21' },
      { id: '20', title: 'Eat stuff', amount: 115, type: 'EXPENSE', transactionDate: '2026-07-22' },
      { id: '21', title: 'New estate Aug', amount: 15000, type: 'EXPENSE', transactionDate: '2026-08-02' },
      { id: '22', title: 'Netflix Aug', amount: 499, type: 'EXPENSE', transactionDate: '2026-08-09' },
    ];

    const result = computeMonthlySpendingTrends(mockTransactions);
    expect(result.items.length).toBe(3);

    // June 2026
    expect(result.items[0].monthKey).toBe('2026-06');
    expect(result.items[0].label).toBe('JUN');
    expect(result.items[0].spent).toBe(133);

    // July 2026
    expect(result.items[1].monthKey).toBe('2026-07');
    expect(result.items[1].label).toBe('JUL');
    expect(result.items[1].spent).toBe(21481.99);

    // August 2026
    expect(result.items[2].monthKey).toBe('2026-08');
    expect(result.items[2].label).toBe('AUG');
    expect(result.items[2].spent).toBe(15499);
  });
});
