'use client';

import { useEffect, useState } from 'react';
import { RefreshCw, Sparkles } from 'lucide-react';
import Skeleton from '@/components/Skeleton';
import { getAiInsights, refreshAiInsights } from '@/lib/api/ai';
import type { AiSummaryResponse } from '@/lib/api/types';
import { formatMonth, getCurrentMonth } from '@/lib/utils';
import { useSettings } from '@/providers/SettingsProvider';

export default function AiInsightsCard() {
  const { currency } = useSettings();
  const [data, setData] = useState<AiSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const month = getCurrentMonth();

  useEffect(() => {
    let mounted = true;

    async function loadInsights() {
      setLoading(true);
      setError('');
      try {
        const result = await getAiInsights(month, currency);
        if (mounted) setData(result);
      } catch (err: unknown) {
        if (mounted) setError(err instanceof Error ? err.message : 'Failed to load AI insights.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadInsights();

    const handleRefresh = () => {
      loadInsights();
    };

    window.addEventListener('spentra-refresh-data', handleRefresh);
    return () => {
      mounted = false;
      window.removeEventListener('spentra-refresh-data', handleRefresh);
    };
  }, [currency, month]);

  async function handleRefreshClick() {
    setRefreshing(true);
    setError('');
    try {
      const result = await refreshAiInsights(month, currency);
      setData(result);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to refresh insights.');
    } finally {
      setRefreshing(false);
    }
  }

  if (loading) {
    return (
      <div className="glass-panel glow-ai rounded-[1.5rem] p-6 sm:p-8 md:col-span-2">
        <Skeleton className="h-6 w-48 mb-4" />
        <Skeleton count={4} className="h-4" />
      </div>
    );
  }

  return (
    <section className="glass-panel glow-ai rounded-[1.5rem] p-6 sm:p-8 md:col-span-2">
      <div className="flex items-start justify-between gap-4 mb-4">
        <div>
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.18em] text-tertiary font-semibold mb-2">
            <Sparkles className="w-4 h-4" />
            AI Monthly Insights - {formatMonth(data?.yearMonth || month)}
          </p>
          <h3 className="text-xl sm:text-2xl font-bold tracking-tight text-on-surface">
            Smart spending summary
          </h3>
        </div>

        {!error && data?.summaryText && data.summaryText !== 'Not enough data yet. Add some transactions and check back!' && (
          <button
            type="button"
            onClick={handleRefreshClick}
            disabled={refreshing}
            className="inline-flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold text-on-surface bg-surface-container-low hover:bg-surface-container-highest transition-all disabled:opacity-60"
          >
            <RefreshCw className={['w-4 h-4', refreshing ? 'animate-spin' : ''].join(' ')} />
            Refresh
          </button>
        )}
      </div>

      {error && (
        <div className="px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
          {error}
        </div>
      )}

      {!error && data?.summaryText === 'Not enough data yet. Add some transactions and check back!' && (
        <div className="rounded-2xl bg-surface-container-low p-5 text-sm text-on-surface-variant">
          Not enough data yet. Add some transactions and check back!
        </div>
      )}

      {!error && data?.summaryText && data.summaryText !== 'Not enough data yet. Add some transactions and check back!' && (
        <>
          <p className="text-sm leading-6 text-on-surface-variant whitespace-pre-line">
            {data.summaryText}
          </p>
          <div className="mt-5 grid grid-cols-2 gap-3 text-xs">
            <div className="rounded-2xl bg-surface-container-low p-4">
              <p className="uppercase tracking-widest text-on-surface-variant mb-1">Total spent</p>
              <p className="font-bold text-on-surface">
                {new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(data.totalSpent)}
              </p>
            </div>
            <div className="rounded-2xl bg-surface-container-low p-4">
              <p className="uppercase tracking-widest text-on-surface-variant mb-1">Top category</p>
              <p className="font-bold text-on-surface">{data.topCategory || 'Uncategorized'}</p>
            </div>
          </div>
        </>
      )}
    </section>
  );
}
