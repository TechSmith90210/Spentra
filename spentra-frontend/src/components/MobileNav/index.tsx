/**
 * @file MobileNav/index.tsx
 * @description Spentra bottom navigation bar for mobile viewports.
 *
 * A fixed-bottom tab bar with four navigation items: Home (Dashboard),
 * Transactions, Budgets, and Account. Hidden on md+ breakpoints where
 * the desktop Navbar takes over.
 *
 * @example
 * ```tsx
 * // Typically placed in the root layout
 * <MobileNav />
 * ```
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, Receipt, PieChart, Settings } from 'lucide-react';
import { type ReactNode } from 'react';

/** A single navigation tab definition */
interface NavItem {
  /** Route path */
  href: string;
  /** Display label */
  label: string;
  /** Lucide icon component */
  icon: ReactNode;
}

/** Mobile navigation items */
const NAV_ITEMS: NavItem[] = [
  { href: '/dashboard', label: 'Home', icon: <LayoutDashboard className="w-5 h-5" /> },
  { href: '/transactions', label: 'Transactions', icon: <Receipt className="w-5 h-5" /> },
  { href: '/budgets', label: 'Budgets', icon: <PieChart className="w-5 h-5" /> },
  { href: '/settings', label: 'Account', icon: <Settings className="w-5 h-5" /> },
];

/**
 * MobileNav — a fixed-bottom tab bar for mobile viewports.
 *
 * Displays four navigation items with icon + label layout.
 * Active state uses on-surface text with font-bold, inactive
 * uses on-surface-variant. Hidden on md+ breakpoints.
 */
export default function MobileNav() {
  const pathname = usePathname();

  return (
    <nav
      className={[
        'md:hidden',
        'fixed bottom-[calc(env(safe-area-inset-bottom)+1rem)] left-4 right-4 z-50',
        'bg-surface/30 backdrop-blur-[24px] backdrop-saturate-[180%]',
        'border border-outline-variant/20 rounded-[2rem]',
        'shadow-[0_8px_32px_rgba(0,0,0,0.08)]',
        'overflow-hidden',
      ].join(' ')}
    >
      <div className="relative flex items-center justify-around h-16">
        {/* Animated Active Indicator Pill */}
        <div
          className="absolute top-2 bottom-2 w-1/4 rounded-[1.5rem] bg-on-surface/[0.08] transition-transform duration-300 ease-out z-0"
          style={{
            transform: `translateX(${Math.max(0, NAV_ITEMS.findIndex(i => i.href === pathname)) * 100}%)`,
            left: 0
          }}
        />

        {NAV_ITEMS.map((item) => {
          const isActive = pathname === item.href;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={[
                'relative z-10 flex flex-col items-center justify-center gap-1',
                'flex-1 h-full',
                'transition-colors duration-200',
                isActive
                  ? 'text-on-surface font-bold'
                  : 'text-on-surface-variant',
              ].join(' ')}
            >
              {item.icon}
              <span className="text-[10px] leading-none">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
