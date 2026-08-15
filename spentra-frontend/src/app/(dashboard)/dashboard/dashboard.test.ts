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
    const createTx = (id: string, title: string, amount: number, type: 'EXPENSE' | 'CREDIT', transactionDate: string): Transaction => ({
      id,
      title,
      amount,
      type,
      transactionDate,
      category: null,
      isRecurring: false,
      recurrence: 'NONE',
      nextExecutionDate: null,
    });

    const mockTransactions: Transaction[] = [
      createTx('1', 'Rapido', 43, 'EXPENSE', '2026-06-28'),
      createTx('2', 'railway ticks', 20, 'EXPENSE', '2026-06-28'),
      createTx('3', 'haircut', 70, 'EXPENSE', '2026-06-28'),
      createTx('4', 'New estate', 15000, 'EXPENSE', '2026-07-02'),
      createTx('5', 'Salary', 35000, 'CREDIT', '2026-07-02'),
      createTx('6', 'Toothbrush and floss', 316, 'EXPENSE', '2026-07-03'),
      createTx('7', 'Pani Puri', 80, 'EXPENSE', '2026-07-04'),
      createTx('8', 'Some chips', 236, 'EXPENSE', '2026-07-06'),
      createTx('9', 'Croffles', 219, 'EXPENSE', '2026-07-07'),
      createTx('10', 'Misc', 149, 'EXPENSE', '2026-07-09'),
      createTx('11', 'Netflix', 499, 'EXPENSE', '2026-07-09'),
      createTx('12', 'shampoo', 682, 'EXPENSE', '2026-07-11'),
      createTx('13', 'RE7', 1259, 'EXPENSE', '2026-07-12'),
      createTx('14', 'Waffles', 222, 'EXPENSE', '2026-07-12'),
      createTx('15', 'Haircut', 700, 'EXPENSE', '2026-07-14'),
      createTx('16', 'Scooter', 1500, 'EXPENSE', '2026-07-17'),
      createTx('17', 'Momos', 60, 'EXPENSE', '2026-07-19'),
      createTx('18', 'Idli', 79.99, 'EXPENSE', '2026-07-21'),
      createTx('19', 'VI Plan', 365, 'EXPENSE', '2026-07-21'),
      createTx('20', 'Eat stuff', 115, 'EXPENSE', '2026-07-22'),
      createTx('21', 'New estate Aug', 15000, 'EXPENSE', '2026-08-02'),
      createTx('22', 'Netflix Aug', 499, 'EXPENSE', '2026-08-09'),
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
