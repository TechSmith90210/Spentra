/**
 * @file Dashboard page — Financial Summary matching Stitch "Summary Dashboard (Spentra)".
 *
 * Features asymmetric grid layout with hero balance card, income/expense
 * summary, spending trends chart, category breakdown, and financial health.
 */

'use client';

import { useState, useEffect, useMemo } from 'react';
import { TrendingUp, TrendingDown, ArrowUpRight, ArrowDownRight, Receipt } from 'lucide-react';
import { getTransactions } from '@/lib/api/transactions';
import { getBudgetSummary } from '@/lib/api/budgets';
import { formatCurrency, getCurrentMonth, formatDate } from '@/lib/utils';
import type { Transaction, BudgetSummary } from '@/lib/api/types';
import { useAuth } from '@/providers/AuthProvider';
import { useSettings } from '@/providers/SettingsProvider';
import Skeleton from '@/components/Skeleton';
import EmptyState from '@/components/EmptyState';
import Badge from '@/components/Badge';

export default function DashboardPage() {
  const { user } = useAuth();
  const { currency } = useSettings();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [budgets, setBudgets] = useState<BudgetSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function fetchData() {
      setLoading(true);
      try {
        const [txData, budgetData] = await Promise.all([
          getTransactions(),
          getBudgetSummary(getCurrentMonth()).catch(() => []),
        ]);
        setTransactions(txData);
        setBudgets(budgetData);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Failed to load data.');
      } finally {
        setLoading(false);
      }
    }
    fetchData();

    const handleRefresh = () => {
      fetchData();
    };

    window.addEventListener('spentra-refresh-data', handleRefresh);
    return () => window.removeEventListener('spentra-refresh-data', handleRefresh);
  }, []);

  /** Computed financial summary */
  const summary = useMemo(() => {
    const totalIncome = transactions
      .filter((t) => t.type === 'CREDIT')
      .reduce((sum, t) => sum + t.amount, 0);
    const totalExpenses = transactions
      .filter((t) => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0);
    const balance = totalIncome - totalExpenses;

    // Category breakdown
    const categoryMap = new Map<string, number>();
    transactions
      .filter((t) => t.type === 'EXPENSE')
      .forEach((t) => {
        const name = t.category?.name || 'Uncategorized';
        categoryMap.set(name, (categoryMap.get(name) || 0) + t.amount);
      });

    const categories = Array.from(categoryMap.entries())
      .map(([name, amount]) => ({ name, amount, percentage: totalExpenses > 0 ? Math.round((amount / totalExpenses) * 100) : 0 }))
      .sort((a, b) => b.amount - a.amount);

    const recentTransactions = [...transactions]
      .sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime())
      .slice(0, 5);

    return { totalIncome, totalExpenses, balance, categories, recentTransactions };
  }, [transactions]);

  /** Computed motivational message */
  const motivation = useMemo(() => {
    if (loading) return '';
    
    const messages = [];
    const hour = new Date().getHours();
    let greeting = 'Good morning';
    if (hour >= 12 && hour < 17) greeting = 'Good afternoon';
    else if (hour >= 17) greeting = 'Good evening';

    const name = user?.name ? user.name.split(' ')[0] : 'there';
    
    if (summary.balance > 1000) {
      messages.push(`${greeting}, ${name}. Your portfolio is looking incredibly strong!`);
      messages.push(`${greeting}, ${name}. Fantastic job keeping your balance high.`);
    } else if (summary.balance > 0) {
      messages.push(`${greeting}, ${name}. You're on the right track, keep it up.`);
    } else if (summary.balance < 0) {
      messages.push(`${greeting}, ${name}. It's a good time to review your recent expenses.`);
    } else {
      messages.push(`${greeting}, ${name}. Ready to take control of your finances?`);
    }

    if (budgets.some(b => b.isExceeded)) {
      messages.push(`Hey ${name}, careful—some of your budgets are running hot.`);
    }

    return messages[Math.floor(Math.random() * messages.length)];
  }, [summary, budgets, loading, user]);

  if (loading) return <DashboardSkeleton />;

  if (error) {
    return (
      <div className="py-16 text-center">
        <p className="text-error mb-4">{error}</p>
        <button onClick={() => window.location.reload()} className="text-tertiary font-semibold hover:underline">
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      {/* Header Section */}
      <header className="flex flex-col md:flex-row md:items-end justify-between mb-12 gap-6">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-on-surface-variant mb-2 font-medium">
            Portfolio Overview
          </p>
          <h1 className="text-4xl md:text-5xl font-bold tracking-tight text-on-surface">
            Financial Summary
          </h1>
          {motivation && (
            <p className="text-sm font-medium text-tertiary mt-3 max-w-md animate-fade-in">{motivation}</p>
          )}
        </div>
        <div className="flex items-center gap-3 px-5 py-3 bg-surface-container-low rounded-xl">
          <span className="text-sm font-semibold text-on-surface">
            {new Date().toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
          </span>
        </div>
      </header>

      {/* The Monolith Cards — Asymmetric Grid */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 mb-12 items-start">
        {/* Hero: Total Balance */}
        <div className="md:col-span-6 lg:col-span-5 bg-surface-container-lowest p-8 rounded-[1.5rem] relative overflow-hidden shadow-sm">
          <div className="relative z-10">
            <p className="text-xs uppercase tracking-widest text-on-surface-variant mb-6 font-medium">
              Total Balance
            </p>
            <h2 className="text-5xl lg:text-6xl font-extrabold tracking-tighter text-on-surface mb-3">
              {formatCurrency(summary.balance, currency)}
            </h2>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-secondary-container/30 text-on-secondary-container rounded-full">
              <TrendingUp className="w-4 h-4" />
              <span className="text-xs font-bold">
                {transactions.length} transactions this period
              </span>
            </div>
          </div>
          {/* Decorative blur */}
          <div className="absolute top-0 right-0 w-32 h-32 bg-tertiary/5 rounded-full blur-3xl -mr-16 -mt-16" />
        </div>

        {/* Income Card */}
        <div className="md:col-span-3 lg:col-span-3 bg-surface-container-low p-8 rounded-[1.5rem] transition-all hover:shadow-sm">
          <p className="text-xs uppercase tracking-widest text-on-surface-variant mb-4 font-medium">Income</p>
          <h3 className="text-3xl font-bold tracking-tight text-on-surface mb-3">
            {formatCurrency(summary.totalIncome, currency)}
          </h3>
          <div className="flex items-center gap-1 text-income">
            <ArrowUpRight className="w-4 h-4" />
            <span className="text-sm font-medium">
              {transactions.filter((t) => t.type === 'CREDIT').length} entries
            </span>
          </div>
        </div>

        {/* Expenses Card */}
        <div className="md:col-span-3 lg:col-span-4 bg-surface-container-low p-8 rounded-[1.5rem] transition-all hover:shadow-sm">
          <p className="text-xs uppercase tracking-widest text-on-surface-variant mb-4 font-medium">Expenses</p>
          <h3 className="text-3xl font-bold tracking-tight text-on-surface mb-3">
            {formatCurrency(summary.totalExpenses, currency)}
          </h3>
          <div className="flex items-center gap-1 text-error">
            <ArrowDownRight className="w-4 h-4" />
            <span className="text-sm font-medium">
              {transactions.filter((t) => t.type === 'EXPENSE').length} entries
            </span>
          </div>
        </div>
      </div>

      {/* Trends Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
        {/* Spending Trends Chart */}
        <div className="lg:col-span-2 bg-surface-container-lowest p-8 rounded-[1.5rem] shadow-sm flex flex-col">
          <div className="flex justify-between items-center mb-10">
            <h4 className="text-xl font-bold tracking-tight text-on-surface">Spending Trends</h4>
            <div className="flex gap-4">
              <span className="flex items-center gap-2 text-xs font-medium text-on-surface-variant">
                <span className="w-2 h-2 rounded-full bg-tertiary" /> Spending
              </span>
              <span className="flex items-center gap-2 text-xs font-medium text-on-surface-variant">
                <span className="w-2 h-2 rounded-full bg-surface-container-highest" /> Budget
              </span>
            </div>
          </div>
          {/* CSS-only bar chart */}
          <div className="flex-grow flex items-end gap-2 h-64 relative">
            <div className="absolute inset-0 flex flex-col justify-between">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="border-b border-surface-container h-0 w-full" />
              ))}
            </div>
            {/* Generate bars from monthly data or static placeholders */}
            {['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'].map((_, i) => {
              const height = Math.max(10, Math.random() * 90);
              const isHighlighted = i === new Date().getMonth();
              return (
                <div
                  key={i}
                  className={`flex-1 rounded-t-lg transition-all ${
                    isHighlighted ? 'bg-tertiary' : 'bg-surface-container-low hover:bg-tertiary/40'
                  }`}
                  style={{ height: `${height}%` }}
                />
              );
            })}
          </div>
          <div className="flex justify-between mt-6 px-1">
            {['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'].map((m) => (
              <span key={m} className="text-[10px] text-on-surface-variant">{m}</span>
            ))}
          </div>
        </div>

        {/* Category Breakdown */}
        <div className="bg-surface-container p-8 rounded-[1.5rem] flex flex-col">
          <h4 className="text-xl font-bold tracking-tight text-on-surface mb-8">Category Breakdown</h4>
          {summary.categories.length === 0 ? (
            <p className="text-sm text-on-surface-variant py-8 text-center">No expense data yet.</p>
          ) : (
            <div className="space-y-6 flex-grow">
              {summary.categories.slice(0, 5).map((cat) => (
                <div key={cat.name}>
                  <div className="flex justify-between mb-2 items-center">
                    <span className="text-sm font-semibold text-on-surface">{cat.name}</span>
                    <span className="text-sm font-bold text-on-surface">{formatCurrency(cat.amount, currency)}</span>
                  </div>
                  <div className="w-full h-1.5 bg-surface-container-lowest rounded-full overflow-hidden">
                    <div
                      className="h-full bg-tertiary rounded-full transition-all duration-500"
                      style={{ width: `${Math.min(cat.percentage, 100)}%` }}
                    />
                  </div>
                  <p className="text-[10px] text-on-surface-variant mt-1.5 uppercase tracking-widest">
                    {cat.percentage}% of spending
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Bottom Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Budget Health */}
        <div className="bg-surface-container-low p-8 rounded-[1.5rem] flex items-center justify-between">
          <div>
            <h5 className="text-lg font-bold mb-1 text-on-surface">Budget Health</h5>
            <p className="text-sm text-on-surface-variant">
              {budgets.filter((b) => b.isExceeded).length === 0
                ? 'All budgets are within limits'
                : `${budgets.filter((b) => b.isExceeded).length} budget(s) exceeded`}
            </p>
          </div>
          <div className="w-16 h-16 rounded-full border-4 border-tertiary border-t-transparent flex items-center justify-center">
            <span className="text-xs font-bold text-on-surface">
              {budgets.length > 0
                ? `${Math.round(
                    ((budgets.length - budgets.filter((b) => b.isExceeded).length) / budgets.length) * 100
                  )}%`
                : '—'}
            </span>
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="bg-surface-container-lowest p-8 rounded-[1.5rem] shadow-sm">
          <h5 className="text-lg font-bold mb-4 text-on-surface">Recent Transactions</h5>
          {summary.recentTransactions.length === 0 ? (
            <p className="text-sm text-on-surface-variant py-4 text-center">No transactions yet.</p>
          ) : (
            <div className="space-y-3">
              {summary.recentTransactions.map((tx) => (
                <div key={tx.id} className="flex items-center justify-between py-2">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-on-surface truncate">{tx.title}</p>
                    <p className="text-xs text-on-surface-variant">{formatDate(tx.transactionDate)}</p>
                  </div>
                  <div className="flex items-center gap-2 ml-4">
                    <span className={`text-sm font-bold ${tx.type === 'CREDIT' ? 'text-income' : 'text-error'}`}>
                      {tx.type === 'CREDIT' ? '+' : '-'}{formatCurrency(tx.amount, currency)}
                    </span>
                    <Badge variant={tx.type === 'CREDIT' ? 'income' : 'expense'} size="sm">
                      {tx.type === 'CREDIT' ? 'Income' : 'Expense'}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/** Skeleton loading state for the dashboard */
function DashboardSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="mb-12">
        <Skeleton className="h-4 w-32 mb-3" />
        <Skeleton className="h-12 w-80" />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 mb-12">
        <div className="md:col-span-5"><Skeleton className="h-48 rounded-[1.5rem]" /></div>
        <div className="md:col-span-3"><Skeleton className="h-48 rounded-[1.5rem]" /></div>
        <div className="md:col-span-4"><Skeleton className="h-48 rounded-[1.5rem]" /></div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2"><Skeleton className="h-96 rounded-[1.5rem]" /></div>
        <Skeleton className="h-96 rounded-[1.5rem]" />
      </div>
    </div>
  );
}
