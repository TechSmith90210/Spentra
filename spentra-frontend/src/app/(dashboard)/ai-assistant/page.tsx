'use client';

import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Camera, Check, Edit3, Send, Sparkles } from 'lucide-react';
import Button from '@/components/Button';
import Input from '@/components/Input';
import Select from '@/components/Select';
import { getCategories } from '@/lib/api/categories';
import { createTransaction } from '@/lib/api/transactions';
import {
  MAX_RECEIPT_SIZE_BYTES,
  parseReceiptImage,
  parseTextPrompt,
  RECEIPT_SIZE_ERROR,
} from '@/lib/api/ai';
import type { Category, TransactionDraft, TransactionType } from '@/lib/api/types';
import { useSettings } from '@/providers/SettingsProvider';

export default function AiAssistantPage() {
  const router = useRouter();
  const { currency } = useSettings();
  const [prompt, setPrompt] = useState('');
  const [categories, setCategories] = useState<Category[]>([]);
  const [draft, setDraft] = useState<TransactionDraft | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [editable, setEditable] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState({
    title: '',
    amount: '',
    type: 'EXPENSE' as TransactionType,
    categoryId: '',
    transactionDate: new Date().toISOString().split('T')[0],
  });

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    if (!draft) return;
    setForm({
      title: draft.title,
      amount: String(draft.amount),
      type: draft.type,
      categoryId: draft.categoryId || '',
      transactionDate: draft.transactionDate,
    });
  }, [draft]);

  const categoryOptions = useMemo(
    () => [{ value: '', label: 'No category' }, ...categories.map((c) => ({ value: c.id, label: c.name }))],
    [categories],
  );

  async function handleParse(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const result = await parseTextPrompt(prompt.trim());
      setDraft(result);
      setEditable(false);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to parse prompt.');
    } finally {
      setLoading(false);
    }
  }

  async function handleReceiptUpload(file: File | null) {
    if (!file) return;
    if (file.size > MAX_RECEIPT_SIZE_BYTES) {
      setError(RECEIPT_SIZE_ERROR);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await parseReceiptImage(file);
      setDraft(result);
      setEditable(false);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to parse receipt.');
    } finally {
      setLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  async function handleConfirm() {
    if (!draft) return;
    setSaving(true);
    setError('');
    try {
      await createTransaction({
        title: form.title.trim(),
        amount: Math.abs(Number(form.amount)),
        type: form.type,
        categoryId: form.categoryId || undefined,
        transactionDate: form.transactionDate,
      });
      window.dispatchEvent(new CustomEvent('spentra-refresh-data'));
      router.back();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to save transaction.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="max-w-3xl mx-auto animate-fade-in">
      <div className="mb-6 flex items-center justify-between gap-4">
        <button
          type="button"
          onClick={() => router.back()}
          className="inline-flex items-center gap-2 text-sm font-semibold text-on-surface-variant hover:text-on-surface"
        >
          <ArrowLeft className="w-4 h-4" />
          Back
        </button>
        <div className="inline-flex items-center gap-2 text-sm font-semibold text-on-surface">
          <Sparkles className="w-4 h-4 text-tertiary" />
          AI Assistant
        </div>
      </div>

      <section className="glass-panel rounded-[1.5rem] p-5 sm:p-6 glow-ai">
        <div className="mb-6">
          <p className="text-sm text-on-surface-variant">
            Type a spending note like &quot;Spent 45 on dinner yesterday&quot; and I’ll turn it into a draft.
          </p>
        </div>

        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/heic"
          capture="environment"
          className="hidden"
          onChange={(e) => handleReceiptUpload(e.target.files?.[0] ?? null)}
        />

        <div className="space-y-4">
          <form onSubmit={handleParse} className="space-y-4">
            <Input
              label="Message"
              placeholder="e.g. Paid 85 for internet bill today"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              disabled={loading || saving}
            />
            <div className="flex flex-col sm:flex-row gap-3 sm:justify-end">
              <Button
                type="button"
                variant="secondary"
                loading={loading}
                disabled={loading || saving}
                icon={<Camera className="w-4 h-4" />}
                onClick={() => fileInputRef.current?.click()}
              >
                Receipt
              </Button>
              <Button
                type="submit"
                loading={loading}
                disabled={prompt.trim().length < 3 || prompt.trim().length > 500 || loading || saving}
                icon={<Send className="w-4 h-4" />}
              >
                Parse
              </Button>
            </div>
          </form>
          <p className="text-xs text-on-surface-variant">
            Upload JPEG, PNG, WebP, or HEIC. Max 2 MB.
          </p>
        </div>

        {error && (
          <div className="mt-4 px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
            {error}
          </div>
        )}

        {draft && (
          <div className="mt-6 glass-panel rounded-[1.25rem] p-4 sm:p-5">
            <div className="flex items-center justify-between gap-4 mb-4">
              <h3 className="text-lg font-bold text-on-surface">Transaction Draft</h3>
              <button
                type="button"
                onClick={() => setEditable((v) => !v)}
                className="inline-flex items-center gap-2 text-sm font-semibold text-tertiary"
              >
                <Edit3 className="w-4 h-4" />
                {editable ? 'Lock' : 'Edit'}
              </button>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Title"
                value={form.title}
                disabled={!editable}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              />
              <Input
                label="Amount"
                value={form.amount}
                disabled={!editable}
                onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value.replace(/,/g, '') }))}
              />
              <Select
                label="Type"
                value={form.type}
                disabled={!editable}
                options={[
                  { value: 'EXPENSE', label: 'Expense' },
                  { value: 'CREDIT', label: 'Credit' },
                ]}
                onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as TransactionType }))}
              />
              <Select
                label="Category"
                value={form.categoryId}
                options={categoryOptions}
                onChange={(e) => setForm((f) => ({ ...f, categoryId: e.target.value }))}
              />
              <Input
                label="Date"
                type="date"
                value={form.transactionDate}
                disabled={!editable}
                onChange={(e) => setForm((f) => ({ ...f, transactionDate: e.target.value }))}
              />
              <Input label="Confidence" value={draft.confidence} disabled />
            </div>

            <div className="mt-5 flex flex-wrap gap-3">
              <Button
                type="button"
                onClick={handleConfirm}
                loading={saving}
                icon={<Check className="w-4 h-4" />}
              >
                Confirm & Add
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setDraft(null);
                  setPrompt('');
                }}
              >
                Discard
              </Button>
            </div>
          </div>
        )}
      </section>

      <p className="mt-4 text-xs text-on-surface-variant">
        Currency display follows your settings ({currency}).
      </p>
    </div>
  );
}
