'use client';

import Link from 'next/link';
import { Sparkles } from 'lucide-react';

export default function LiquidQuickAddChip() {
  return (
    <Link
      href="/ai-assistant"
      className={[
        'md:hidden fixed left-1/2 -translate-x-1/2 z-50',
        'bottom-[calc(env(safe-area-inset-bottom)+5.5rem)]',
        'glass-chip glow-ai rounded-full px-4 py-2.5',
        'flex items-center gap-2 cursor-pointer',
        'transition-all duration-200 hover:scale-105 active:scale-95',
      ].join(' ')}
      aria-label="Open AI assistant"
    >
      <Sparkles className="w-4 h-4 text-tertiary" />
      <span className="text-sm font-semibold text-on-surface">Ask AI</span>
    </Link>
  );
}
