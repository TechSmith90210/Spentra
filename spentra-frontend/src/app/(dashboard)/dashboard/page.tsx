/**
 * @file Dashboard page — Financial Summary matching Stitch "Summary Dashboard (Spentra)".
 *
 * Features asymmetric grid layout with hero balance card, income/expense
 * summary, spending trends chart, category breakdown, and financial health.
 */

'use client';

import { useState, useEffect, useMemo } from 'react';
import { TrendingUp, TrendingDown, ArrowUpRight, ArrowDownRight, Receipt, Sparkles } from 'lucide-react';
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

  /** Computed financial insights */
  const insights = useMemo(() => {
    if (loading) return [];
    const list: string[] = [];

    // 1. Critical / Budget Exceeded
    const exceeded = budgets.filter((b) => b.isExceeded);
    if (exceeded.length > 0) {
      if (exceeded.length === 1) {
        list.push(`You've exceeded your "${exceeded[0].categoryName}" budget by ${formatCurrency(exceeded[0].actualSpent - exceeded[0].amountLimit, currency)}.`);
      } else {
        list.push(`You have exceeded budgets in ${exceeded.length} categories: ${exceeded.map(b => `"${b.categoryName}"`).join(', ')}.`);
      }
    }

    // 2. Budget Warnings (running hot > 85%)
    const hot = budgets.filter((b) => !b.isExceeded && b.amountLimit > 0 && (b.actualSpent / b.amountLimit) >= 0.85);
    if (hot.length > 0) {
      if (hot.length === 1) {
        list.push(`Your "${hot[0].categoryName}" budget is running hot at ${Math.round((hot[0].actualSpent / hot[0].amountLimit) * 100)}% of its limit (${formatCurrency(hot[0].remaining, currency)} remaining).`);
      } else {
        list.push(`Budgets for ${hot.map(b => `"${b.categoryName}"`).join(', ')} are running hot (over 85% spent).`);
      }
    }

    // 3. High Savings Rate
    if (summary.totalIncome > 0 && summary.totalExpenses < summary.totalIncome) {
      const netSavings = summary.totalIncome - summary.totalExpenses;
      const savingsRate = Math.round((netSavings / summary.totalIncome) * 100);
      if (savingsRate >= 20) {
        list.push(`Incredible work! You've saved ${savingsRate}% of your income (${formatCurrency(netSavings, currency)}) so far this month.`);
        list.push(`Your savings rate is looking spectacular at ${savingsRate}% this month!`);
      }
    }

    // 4. Deficit Alert
    if (summary.totalExpenses > summary.totalIncome && summary.totalIncome > 0) {
      const deficit = summary.totalExpenses - summary.totalIncome;
      list.push(`Your expenses are outpacing your income by ${formatCurrency(deficit, currency)} this month. Let's see where we can trim back.`);
    }

    // 5. Top Spend Category
    if (summary.categories.length > 0 && summary.totalExpenses > 0) {
      const topCat = summary.categories[0];
      list.push(`Your biggest spending category this month is "${topCat.name}" at ${formatCurrency(topCat.amount, currency)} (${topCat.percentage}% of all expenses).`);
    }

    // 6. Budgets all healthy
    if (budgets.length > 0 && exceeded.length === 0 && hot.length === 0) {
      list.push(`All of your active budgets are beautifully in the green! Magnificent money management.`);
    }

    // 7. Portfolio surplus
    if (summary.balance > 1000) {
      list.push(`Your portfolio is looking incredibly strong with a net surplus of ${formatCurrency(summary.balance, currency)}.`);
      list.push(`Fantastic job keeping your net balance high at ${formatCurrency(summary.balance, currency)}.`);
    } else if (summary.balance > 0) {
      list.push(`Your portfolio is in the green with a surplus of ${formatCurrency(summary.balance, currency)}. Keep up the steady progress.`);
    } else if (summary.balance < 0) {
      list.push(`Your balance is currently in the negative by ${formatCurrency(Math.abs(summary.balance), currency)}. It's a good time to review your recent expenses.`);
    } else {
      list.push(`Ready to take control of your finances? Start tracking your daily habits to build wealth.`);
    }

    // 8. No transactions
    if (transactions.length === 0) {
      return [`Welcome to Spentra! Ready to take control? Start by logging your first transaction to unlock deep insights.`];
    }

    return list;
  }, [summary, budgets, loading, currency, transactions.length]);

  const [insightIndex, setInsightIndex] = useState(0);

  // Reset/clamp index when insights length changes
  useEffect(() => {
    setInsightIndex(0);
  }, [insights.length]);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    const name = user?.name ? user.name.split(' ')[0] : 'there';
    if (hour >= 5 && hour < 12) return `Good morning, ${name}!`;
    if (hour >= 12 && hour < 17) return `Good afternoon, ${name}!`;
    if (hour >= 17 && hour < 22) return `Good evening, ${name}!`;
    return `Burning the midnight oil, ${name}?`;
  }, [user]);

  const activeInsight = insights[insightIndex] || '';

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
          {activeInsight && (
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5 mt-3 text-sm font-medium text-tertiary select-none">
              <span className="font-semibold text-on-surface">
                {greeting}
              </span>
              <span className="text-tertiary/90 transition-all duration-300 animate-fade-in" key={insightIndex}>
                {activeInsight}
              </span>
              {insights.length > 1 && (
                <button
                  onClick={() => setInsightIndex((prev) => (prev + 1) % insights.length)}
                  className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-surface-container-low hover:bg-surface-container-highest border border-surface-container-highest rounded-full text-[10px] text-tertiary/70 hover:text-tertiary transition-all cursor-pointer shadow-sm active:scale-95"
                  title="Cycle financial insights"
                >
                  <Sparkles className="w-3 h-3 text-tertiary/60" />
                  <span>Insight {insightIndex + 1}/{insights.length}</span>
                </button>
              )}
            </div>
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
        <div className="md:col-span-6 lg:col-span-5 bg-surface-container-lowest p-8 rounded-[1.5rem] relative overflow-hidden shadow-sm animate-slide-up stagger-1 hover:-translate-y-1 hover:scale-[1.01] hover:shadow-md transition-all duration-300 ease-out" style={{ animationFillMode: "both" }}>
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
        <div className="md:col-span-3 lg:col-span-3 bg-surface-container-low p-8 rounded-[1.5rem] transition-all duration-300 hover:shadow-md animate-slide-up stagger-2 hover:-translate-y-1 hover:scale-[1.01] ease-out" style={{ animationFillMode: "both" }}>
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
        <div className="md:col-span-3 lg:col-span-4 bg-surface-container-low p-8 rounded-[1.5rem] transition-all duration-300 hover:shadow-md animate-slide-up stagger-3 hover:-translate-y-1 hover:scale-[1.01] ease-out" style={{ animationFillMode: "both" }}>
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
        <div className="lg:col-span-2 bg-surface-container-lowest p-8 rounded-[1.5rem] shadow-sm flex flex-col animate-slide-up stagger-4 hover:-translate-y-0.5 transition-all duration-300 ease-out" style={{ animationFillMode: "both" }}>
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
        <div className="bg-surface-container p-8 rounded-[1.5rem] flex flex-col animate-slide-up stagger-5 hover:-translate-y-0.5 transition-all duration-300 ease-out" style={{ animationFillMode: "both" }}>
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
        <div className="bg-surface-container-low p-8 rounded-[1.5rem] flex items-center justify-between animate-slide-up stagger-6 hover:-translate-y-0.5 transition-all duration-300 ease-out" style={{ animationFillMode: "both" }}>
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
        <div className="bg-surface-container-lowest p-8 rounded-[1.5rem] shadow-sm animate-slide-up stagger-6 hover:-translate-y-0.5 transition-all duration-300 ease-out" style={{ animationFillMode: "both" }}>
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
