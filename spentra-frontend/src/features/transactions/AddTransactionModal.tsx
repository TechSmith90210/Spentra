/**
 * @file AddTransactionModal — create or edit a transaction.
 */

'use client';

import { useState, useEffect, type FormEvent } from 'react';
import Modal from '@/components/Modal';
import Input from '@/components/Input';
import Select from '@/components/Select';
import Button from '@/components/Button';
import { getCategories } from '@/lib/api/categories';
import { createTransaction, updateTransaction, deleteTransaction } from '@/lib/api/transactions';
import type { Category, Transaction, TransactionType, Recurrence } from '@/lib/api/types';
import { useSettings } from '@/providers/SettingsProvider';
import { formatInputAmount } from '@/lib/utils';

interface AddTransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  /** If provided, the modal opens in edit mode */
  transaction?: Transaction | null;
}

const TYPE_OPTIONS = [
  { value: 'EXPENSE', label: 'Expense' },
  { value: 'CREDIT', label: 'Income / Credit' },
];

const RECURRENCE_OPTIONS = [
  { value: 'NONE', label: 'None' },
  { value: 'DAILY', label: 'Daily' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'YEARLY', label: 'Yearly' },
];

export default function AddTransactionModal({
  isOpen,
  onClose,
  onSuccess,
  transaction,
}: AddTransactionModalProps) {
  const { settings } = useSettings();
  const [categories, setCategories] = useState<Category[]>([]);
  const isEditing = !!transaction;

  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<TransactionType>('EXPENSE');
  const [categoryId, setCategoryId] = useState('');
  const [transactionDate, setTransactionDate] = useState('');
  const [isRecurring, setIsRecurring] = useState(false);
  const [recurrence, setRecurrence] = useState<Recurrence>('NONE');
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  // Fetch categories when modal opens
  useEffect(() => {
    if (isOpen) {
      getCategories()
        .then(setCategories)
        .catch(() => setCategories([]));
    }
  }, [isOpen]);

  // Populate form when editing
  useEffect(() => {
    if (transaction) {
      setTitle(transaction.title);
      setAmount(String(transaction.amount));
      setType(transaction.type);
      setCategoryId(transaction.category?.id || '');
      setTransactionDate(transaction.transactionDate);
      setIsRecurring(transaction.isRecurring);
      setRecurrence(transaction.recurrence);
    } else {
      setTitle('');
      setAmount('');
      setType('EXPENSE');
      setCategoryId('');
      setTransactionDate(new Date().toISOString().split('T')[0]);
      setIsRecurring(false);
      setRecurrence('NONE');
    }
    setError('');
  }, [transaction, isOpen]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const payload = {
        title,
        amount: parseFloat(amount),
        type,
        categoryId: categoryId || undefined,
        transactionDate,
        isRecurring,
        recurrence: isRecurring ? recurrence : ('NONE' as Recurrence),
      };

      if (isEditing && transaction) {
        await updateTransaction(transaction.id, payload);
      } else {
        await createTransaction(payload);
      }
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong.');
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!transaction) return;
    setDeleting(true);
    try {
      await deleteTransaction(transaction.id);
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to delete.');
    } finally {
      setDeleting(false);
    }
  }

  const categoryOptions = [
    { value: '', label: 'No category' },
    ...categories.map((c) => ({ value: c.id, label: c.name })),
  ];

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Edit Transaction' : 'New Transaction'}
      size="md"
    >
      {error && (
        <div className="mb-4 px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <Input
          label="Title"
          placeholder="e.g. Grocery Shopping"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          name="title"
        />

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Amount"
            type="text"
            inputMode="decimal"
            placeholder="0.00"
            value={formatInputAmount(amount, settings.currency)}
            onChange={(e) => {
              const raw = e.target.value.replace(/,/g, '');
              if (/^-?\d*\.?\d*$/.test(raw)) {
                setAmount(raw);
              }
            }}
            required
            name="amount"
          />
          <Select
            label="Type"
            options={TYPE_OPTIONS}
            value={type}
            onChange={(e) => setType(e.target.value as TransactionType)}
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Category"
            options={categoryOptions}
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            placeholder="Select category"
          />
          <Input
            label="Date"
            type="date"
            value={transactionDate}
            onChange={(e) => setTransactionDate(e.target.value)}
            name="transactionDate"
          />
        </div>

        {/* Recurring toggle */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            role="switch"
            aria-checked={isRecurring}
            onClick={() => setIsRecurring(!isRecurring)}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors cursor-pointer ${
              isRecurring ? 'bg-tertiary' : 'bg-surface-container-high'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                isRecurring ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
          <span className="text-sm text-on-surface">Recurring transaction</span>
        </div>

        {isRecurring && (
          <Select
            label="Recurrence"
            options={RECURRENCE_OPTIONS}
            value={recurrence}
            onChange={(e) => setRecurrence(e.target.value as Recurrence)}
          />
        )}

        <div className="flex gap-3 pt-2">
          {isEditing && (
            <Button
              type="button"
              variant="danger"
              onClick={handleDelete}
              loading={deleting}
            >
              Delete
            </Button>
          )}
          <Button type="submit" variant="primary" fullWidth loading={loading}>
            {isEditing ? 'Save Changes' : 'Add Transaction'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
