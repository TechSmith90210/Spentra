'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Sparkles } from 'lucide-react';

export default function LiquidQuickAddChip() {
  const pathname = usePathname();

  // Hide the chip if user is already on the AI Assistant page
  if (pathname === '/ai-assistant') {
    return null;
  }

  return (
    <div className="md:hidden fixed bottom-[calc(env(safe-area-inset-bottom)+5.5rem)] inset-x-0 flex justify-center items-center pointer-events-none z-40">
      <Link
        href="/ai-assistant"
        className={[
          'pointer-events-auto',
          'w-auto min-w-[124px] max-w-fit',
          'liquid-glass-chip glow-ai rounded-full px-5 py-2.5',
          'flex items-center justify-center gap-2.5 cursor-pointer',
          'transition-all duration-300 hover:scale-105 active:scale-95',
          'group select-none shadow-[0_10px_28px_-4px_rgba(74,75,215,0.3)]',
        ].join(' ')}
        aria-label="Open AI assistant"
      >
        <div className="relative flex items-center justify-center">
          <Sparkles className="w-4 h-4 text-tertiary transition-transform duration-300 group-hover:rotate-12 group-hover:scale-110" />
          <span className="absolute -top-0.5 -right-0.5 w-1.5 h-1.5 bg-tertiary rounded-full animate-ping opacity-75" />
        </div>
        <span className="text-xs sm:text-sm font-bold tracking-wide text-on-surface">
          Ask AI
        </span>
      </Link>
    </div>
  );
}
