'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, Receipt, Sparkles, PieChart, Settings } from 'lucide-react';
import { type ReactNode } from 'react';

/** Navigation item definition */
interface NavItem {
  href: string;
  label: string;
  icon: ReactNode;
}

/** 4 core navigation items */
const NAV_ITEMS: NavItem[] = [
  { href: '/dashboard', label: 'Home', icon: <LayoutDashboard className="w-5 h-5" /> },
  { href: '/transactions', label: 'Transactions', icon: <Receipt className="w-5 h-5" /> },
  { href: '/budgets', label: 'Budgets', icon: <PieChart className="w-5 h-5" /> },
  { href: '/settings', label: 'Account', icon: <Settings className="w-5 h-5" /> },
];

export default function MobileNav() {
  const pathname = usePathname();
  const activeIndex = NAV_ITEMS.findIndex((item) => item.href === pathname);

  return (
    <nav className="md:hidden fixed bottom-[calc(env(safe-area-inset-bottom,0px)+0.75rem)] inset-x-0 z-50 flex flex-col items-center pointer-events-none px-4">
      {/* ── 1. Floating Ask AI Action Button (FAB) ── */}
      {pathname !== '/ai-assistant' && (
        <div className="flex justify-center mb-2.5 pointer-events-auto">
          <Link
            href="/ai-assistant"
            className={[
              'apple-liquid-fab rounded-full px-5 py-2.5 flex items-center gap-2 select-none cursor-pointer',
              'transition-all duration-150 ease-out active:scale-[0.93] focus:outline-none',
            ].join(' ')}
            aria-label="Open AI Assistant"
          >
            <Sparkles className="w-4 h-4 text-indigo-500 dark:text-indigo-400" />
            <span className="text-xs font-bold tracking-wide text-on-surface">
              Ask AI
            </span>
          </Link>
        </div>
      )}

      {/* ── 2. Floating Liquid Glass Navigation Bar ── */}
      <div className="pointer-events-auto w-full max-w-[380px] apple-liquid-nav rounded-full p-1 relative shadow-[0_16px_40px_-8px_rgba(15,23,42,0.12)]">
        <div className="relative grid grid-cols-4 items-center h-13">
          {/* Physical Sliding Liquid Glass Indicator */}
          <div
            className={[
              'absolute inset-y-0.5 left-0 z-0 pointer-events-none p-0.5',
              'transition-all duration-250 ease-[cubic-bezier(0.25,1,0.5,1)] will-change-transform',
              activeIndex === -1 ? 'opacity-0 scale-95' : 'opacity-100 scale-100',
            ].join(' ')}
            style={{
              width: '25%',
              transform: `translateX(${Math.max(0, activeIndex) * 100}%)`,
            }}
          >
            <div className="w-full h-full rounded-full apple-sliding-indicator" />
          </div>

          {/* 4 Navigation Items */}
          {NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={[
                  'relative z-10 w-full h-full flex flex-col items-center justify-center gap-0.5 py-1 px-1 select-none focus:outline-none',
                  'transition-transform duration-100 active:scale-90',
                ].join(' ')}
              >
                <div
                  className={[
                    'transition-all duration-200',
                    isActive
                      ? 'text-on-surface scale-105'
                      : 'text-on-surface-variant/75 hover:text-on-surface',
                  ].join(' ')}
                >
                  {item.icon}
                </div>
                <span
                  className={[
                    'text-[10px] tracking-tight leading-none transition-colors duration-200',
                    isActive ? 'font-bold text-on-surface' : 'font-medium text-on-surface-variant/75',
                  ].join(' ')}
                >
                  {item.label}
                </span>
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}
