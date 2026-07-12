/**
 * @file Transactions page — list, filter, search, edit transactions.
 * Matches Stitch "Transaction Tracker (Spentra)" design.
 */

'use client';

import { useState, useEffect, useMemo } from 'react';
import { Search, Receipt, ArrowUpRight, ArrowDownRight, Repeat } from 'lucide-react';
import { getTransactions } from '@/lib/api/transactions';
import { getCategories } from '@/lib/api/categories';
import { useSettings } from '@/providers/SettingsProvider';
import { formatCurrency, formatDate } from '@/lib/utils';
import type { Transaction, Category, TransactionType } from '@/lib/api/types';
import { useDebounce } from '@/hooks/useDebounce';
import Input from '@/components/Input';
import Badge from '@/components/Badge';
import Skeleton from '@/components/Skeleton';
import EmptyState from '@/components/EmptyState';
import AddTransactionModal from '@/features/transactions/AddTransactionModal';

type FilterType = 'ALL' | TransactionType;

export default function TransactionsPage() {
  const { currency } = useSettings();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<FilterType>('ALL');
  const [filterCategory, setFilterCategory] = useState('');
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);

  const debouncedSearch = useDebounce(searchQuery, 300);

  async function fetchData() {
    setLoading(true);
    try {
      const [txData, catData] = await Promise.all([
        getTransactions(),
        getCategories(),
      ]);
      setTransactions(txData);
      setCategories(catData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchData();
    window.addEventListener('spentra-refresh-data', fetchData);
    return () => window.removeEventListener('spentra-refresh-data', fetchData);
  }, []);

  /** Filtered transactions */
  const filtered = useMemo(() => {
    let result = transactions;

    // Search
    if (debouncedSearch) {
      const q = debouncedSearch.toLowerCase();
      result = result.filter(
        (t) =>
          t.title.toLowerCase().includes(q) ||
          t.category?.name.toLowerCase().includes(q)
      );
    }

    // Type filter
    if (filterType !== 'ALL') {
      result = result.filter((t) => t.type === filterType);
    }

    // Category filter
    if (filterCategory) {
      result = result.filter((t) => t.category?.id === filterCategory);
    }

    // Sort by date descending
    return result.sort(
      (a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()
    );
  }, [transactions, debouncedSearch, filterType, filterCategory]);

  function handleEdit(tx: Transaction) {
    setEditingTransaction(tx);
    setShowEditModal(true);
  }

  function handleEditSuccess() {
    setShowEditModal(false);
    setEditingTransaction(null);
    fetchData();
  }

  if (error && !loading) {
    return (
      <div className="py-16 text-center">
        <p className="text-error mb-4">{error}</p>
        <button onClick={fetchData} className="text-tertiary font-semibold hover:underline cursor-pointer">
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <header className="mb-10">
        <h1 className="text-4xl font-bold tracking-tight text-on-surface mb-2">
          Transaction Tracker
        </h1>
        <p className="text-sm text-on-surface-variant">
          {loading ? '...' : `${transactions.length} total transactions`}
        </p>
      </header>

      {/* Filters */}
      <div className="flex flex-col md:flex-row gap-4 mb-8">
        <div className="flex-1 max-w-md">
          <Input
            placeholder="Search transactions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            icon={<Search className="w-4 h-4" />}
            name="search"
          />
        </div>

        {/* Type filter pills */}
        <div className="flex items-center gap-2">
          {(['ALL', 'EXPENSE', 'CREDIT'] as FilterType[]).map((type) => (
            <button
              key={type}
              onClick={() => setFilterType(type)}
              className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all duration-200 cursor-pointer hover:-translate-y-0.5 hover:scale-[1.03] active:scale-[0.97] ${
                filterType === type
                  ? 'bg-on-surface text-surface'
                  : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
              }`}
            >
              {type === 'ALL' ? 'All' : type === 'EXPENSE' ? 'Expenses' : 'Income'}
            </button>
          ))}
        </div>

        {/* Category filter */}
        {categories.length > 0 && (
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="px-4 py-2 bg-surface-container-low rounded-xl text-sm text-on-surface border-none outline-none appearance-none cursor-pointer"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        )}
      </div>

      {/* Transaction List */}
      {loading ? (
        <div className="space-y-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-20 rounded-[1.5rem]" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<Receipt />}
          title="No transactions found"
          description={
            debouncedSearch || filterType !== 'ALL' || filterCategory
              ? 'Try adjusting your filters'
              : 'Add your first transaction to get started'
          }
        />
      ) : (
        <div className="space-y-4">
          {filtered.map((tx, i) => (
            <button
              key={tx.id}
              onClick={() => handleEdit(tx)}
              className={`w-full text-left bg-surface-container-lowest p-5 rounded-[1.5rem] shadow-sm hover:shadow-md hover:-translate-y-0.5 hover:scale-[1.005] transition-all duration-300 ease-out cursor-pointer active:scale-[0.99] animate-slide-up stagger-${Math.min(i + 1, 6)}`}
              style={{ animationFillMode: 'both' }}
            >
              <div className="flex items-center gap-4">
                {/* Icon */}
                <div className={`p-3 rounded-xl ${
                  tx.type === 'CREDIT'
                    ? 'bg-income-bg text-income'
                    : 'bg-surface-container-low text-on-surface-variant'
                }`}>
                  {tx.type === 'CREDIT'
                    ? <ArrowUpRight className="w-5 h-5" />
                    : <ArrowDownRight className="w-5 h-5" />}
                </div>

                {/* Details */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-on-surface truncate">{tx.title}</p>
                  <p className="text-xs text-on-surface-variant mt-0.5">
                    {tx.category?.name || 'Uncategorized'} · {formatDate(tx.transactionDate)}
                  </p>
                </div>

                {/* Amount & Badges */}
                <div className="flex items-center gap-3 ml-4 shrink-0">
                    <span className={`text-sm font-bold ${tx.type === 'CREDIT' ? 'text-income' : 'text-error'}`}>
                      {tx.type === 'CREDIT' ? '+' : '-'}{formatCurrency(tx.amount, currency)}
                    </span>
                  {tx.isRecurring && (
                    <Badge variant="recurring" size="sm">
                      <Repeat className="w-3 h-3 mr-1 inline" />
                      {tx.recurrence}
                    </Badge>
                  )}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* Edit modal */}
      <AddTransactionModal
        isOpen={showEditModal}
        onClose={() => { setShowEditModal(false); setEditingTransaction(null); }}
        onSuccess={handleEditSuccess}
        transaction={editingTransaction}
      />
    </div>
  );
}
