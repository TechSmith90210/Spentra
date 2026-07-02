/**
 * @file Budgets page — budget overview and recurring transactions.
 * Matches Stitch "Budgeting & Recurring (Spentra)" design.
 */

'use client';

import { useState, useEffect, useMemo } from 'react';
import { ChevronLeft, ChevronRight, Plus, PieChart, Repeat, AlertTriangle } from 'lucide-react';
import { getBudgetSummary, setBudget } from '@/lib/api/budgets';
import { getTransactions } from '@/lib/api/transactions';
import { getCategories } from '@/lib/api/categories';
import { useSettings } from '@/providers/SettingsProvider';
import { formatCurrency, formatMonth, getCurrentMonth, formatInputAmount } from '@/lib/utils';
import type { BudgetSummary, Transaction, Category, CreateBudgetRequest } from '@/lib/api/types';
import ProgressBar from '@/components/ProgressBar';
import Skeleton from '@/components/Skeleton';
import EmptyState from '@/components/EmptyState';
import Badge from '@/components/Badge';
import Modal from '@/components/Modal';
import Input from '@/components/Input';
import Select from '@/components/Select';
import Button from '@/components/Button';

export default function BudgetsPage() {
  const { currency } = useSettings();
  const [month, setMonth] = useState(getCurrentMonth());
  const [budgets, setBudgets] = useState<BudgetSummary[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddBudget, setShowAddBudget] = useState(false);

  async function fetchData() {
    setLoading(true);
    try {
      const [budgetData, txData, catData] = await Promise.all([
        getBudgetSummary(month).catch(() => []),
        getTransactions(),
        getCategories(),
      ]);
      setBudgets(budgetData);
      setTransactions(txData);
      setCategories(catData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load data.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchData();
    window.addEventListener('spentra-refresh-data', fetchData);
    return () => window.removeEventListener('spentra-refresh-data', fetchData);
  }, [month]);

  /** Navigate months */
  function prevMonth() {
    const [y, m] = month.split('-').map(Number);
    const d = new Date(y, m - 2, 1);
    setMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  function nextMonth() {
    const [y, m] = month.split('-').map(Number);
    const d = new Date(y, m, 1);
    setMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  /** Recurring transactions */
  const recurringTransactions = useMemo(
    () => transactions.filter((t) => t.isRecurring),
    [transactions]
  );

  if (error && !loading) {
    return (
      <div className="py-16 text-center">
        <p className="text-error mb-4">{error}</p>
        <button onClick={fetchData} className="text-tertiary font-semibold hover:underline cursor-pointer">Retry</button>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <header className="flex flex-col md:flex-row md:items-end justify-between mb-10 gap-6">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-on-surface-variant mb-2 font-medium">
            Financial Planning
          </p>
          <h1 className="text-4xl font-bold tracking-tight text-on-surface">
            Budgets & Recurring
          </h1>
        </div>

        {/* Month selector */}
        <div className="flex items-center gap-2">
          <button onClick={prevMonth} className="p-2 rounded-lg hover:bg-surface-container-low transition-all cursor-pointer active:scale-95">
            <ChevronLeft className="w-5 h-5 text-on-surface-variant" />
          </button>
          <span className="px-4 py-2 bg-surface-container-low rounded-xl text-sm font-semibold text-on-surface min-w-[140px] text-center">
            {formatMonth(month)}
          </span>
          <button onClick={nextMonth} className="p-2 rounded-lg hover:bg-surface-container-low transition-all cursor-pointer active:scale-95">
            <ChevronRight className="w-5 h-5 text-on-surface-variant" />
          </button>
        </div>
      </header>

      {/* Budget Cards Grid */}
      <section className="mb-12">
        <h2 className="text-xs uppercase tracking-widest text-on-surface-variant mb-6 font-medium">
          Budget Overview
        </h2>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-48 rounded-[1.5rem]" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {budgets.map((budget) => {
              const percentage = budget.amountLimit > 0
                ? Math.round((budget.actualSpent / budget.amountLimit) * 100)
                : 0;

              return (
                <div
                  key={budget.budgetId}
                  className={`bg-surface-container-lowest p-6 rounded-[1.5rem] shadow-sm transition-all ${
                    budget.isExceeded ? 'ring-1 ring-error/20' : ''
                  }`}
                >
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-sm font-semibold text-on-surface">
                      {budget.categoryName}
                    </h3>
                    {budget.isExceeded && (
                      <AlertTriangle className="w-4 h-4 text-error" />
                    )}
                  </div>

                  <ProgressBar
                    value={percentage}
                    variant={
                      percentage > 90 ? 'danger' : percentage > 70 ? 'warning' : 'default'
                    }
                  />

                  <div className="flex justify-between mt-4 text-xs">
                    <span className="text-on-surface-variant">
                      Spent: <span className="font-bold text-on-surface">{formatCurrency(budget.actualSpent, currency)}</span>
                    </span>
                    <span className="text-on-surface-variant">
                      Limit: <span className="font-bold text-on-surface">{formatCurrency(budget.amountLimit, currency)}</span>
                    </span>
                  </div>

                  <p className={`text-xs mt-2 font-medium ${budget.isExceeded ? 'text-error' : 'text-income'}`}>
                    {budget.isExceeded
                      ? `Over by ${formatCurrency(Math.abs(budget.remaining), currency)}`
                      : `${formatCurrency(budget.remaining, currency)} remaining`}
                  </p>
                </div>
              );
            })}

            {/* Add Budget Card */}
            <button
              onClick={() => setShowAddBudget(true)}
              className="border-2 border-dashed border-outline-variant/30 rounded-[1.5rem] p-6 flex flex-col items-center justify-center gap-3 hover:border-tertiary/40 hover:bg-tertiary/5 transition-all cursor-pointer min-h-[192px] active:scale-[0.98]"
            >
              <div className="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center">
                <Plus className="w-6 h-6 text-on-surface-variant" />
              </div>
              <span className="text-sm font-semibold text-on-surface-variant">Add Budget</span>
            </button>
          </div>
        )}
      </section>

      {/* Recurring Transactions */}
      <section>
        <h2 className="text-xs uppercase tracking-widest text-on-surface-variant mb-6 font-medium">
          Recurring Transactions
        </h2>

        {loading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-20 rounded-[1.5rem]" />
            ))}
          </div>
        ) : recurringTransactions.length === 0 ? (
          <EmptyState
            icon={<Repeat />}
            title="No recurring transactions"
            description="Recurring transactions will appear here"
          />
        ) : (
          <div className="space-y-4">
            {recurringTransactions.map((tx) => (
              <div
                key={tx.id}
                className="bg-surface-container-lowest p-5 rounded-[1.5rem] shadow-sm flex items-center gap-4"
              >
                <div className="p-3 rounded-xl bg-tertiary/10 text-tertiary">
                  <Repeat className="w-5 h-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-on-surface truncate">{tx.title}</p>
                  <p className="text-xs text-on-surface-variant mt-0.5">
                    {tx.category?.name || 'Uncategorized'} · {tx.recurrence.toLowerCase()}
                  </p>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span className={`text-sm font-bold ${tx.type === 'CREDIT' ? 'text-income' : 'text-on-surface'}`}>
                    {formatCurrency(tx.amount, currency)}
                  </span>
                  <Badge variant="recurring" size="sm">{tx.recurrence}</Badge>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Set Budget Modal */}
      <SetBudgetModal
        isOpen={showAddBudget}
        onClose={() => setShowAddBudget(false)}
        categories={categories}
        month={month}
        onSuccess={() => { setShowAddBudget(false); fetchData(); }}
      />
    </div>
  );
}

/** Modal for adding/editing a budget */
function SetBudgetModal({
  isOpen,
  onClose,
  categories,
  month,
  onSuccess,
}: {
  isOpen: boolean;
  onClose: () => void;
  categories: Category[];
  month: string;
  onSuccess: () => void;
}) {
  const { currency } = useSettings();
  const [categoryId, setCategoryId] = useState('');
  const [amountLimit, setAmountLimit] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setCategoryId('');
      setAmountLimit('');
      setError('');
    }
  }, [isOpen]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const payload: CreateBudgetRequest = {
        amountLimit: parseFloat(amountLimit),
        categoryId: categoryId || undefined,
        budgetMonth: month,
      };
      await setBudget(payload);
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to set budget.');
    } finally {
      setLoading(false);
    }
  }

  const categoryOptions = [
    { value: '', label: 'Global (All Categories)' },
    ...categories.map((c) => ({ value: c.id, label: c.name })),
  ];

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Set Budget" size="sm">
      {error && (
        <div className="mb-4 px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
          {error}
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-5">
        <Select
          label="Category"
          options={categoryOptions}
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
        />
        <Input
          label="Budget Limit"
          type="text"
          inputMode="decimal"
          placeholder="500.00"
          value={formatInputAmount(amountLimit, currency)}
          onChange={(e) => {
            const raw = e.target.value.replace(/,/g, '');
            if (/^-?\d*\.?\d*$/.test(raw)) {
              setAmountLimit(raw);
            }
          }}
          required
          name="amountLimit"
        />
        <p className="text-xs text-on-surface-variant">
          Budget for: <span className="font-semibold text-on-surface">{formatMonth(month)}</span>
        </p>
        <Button type="submit" variant="primary" fullWidth loading={loading}>
          Set Budget
        </Button>
      </form>
    </Modal>
  );
}
