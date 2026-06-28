/**
 * @file Navbar/index.tsx
 * @description Spentra top navigation bar.
 *
 * A fixed-top glass-effect navbar with the Spentra logo, desktop navigation
 * links, theme toggle, "New Entry" CTA, and user avatar. Hides nav links
 * on mobile (replaced by MobileNav at the bottom).
 *
 * @example
 * ```tsx
 * <Navbar onNewEntry={() => setShowModal(true)} />
 * ```
 */

'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Plus } from 'lucide-react';
import ThemeToggle from '@/components/ThemeToggle';
import Avatar from '@/components/Avatar';
import { useAuth } from '@/providers/AuthProvider';

/** Props for the Navbar component */
export interface NavbarProps {
  /** Callback fired when the "New Entry" button is clicked */
  onNewEntry?: () => void;
}

/** Desktop navigation links */
const NAV_LINKS = [
  { href: '/dashboard', label: 'Dashboard' },
  { href: '/transactions', label: 'Transactions' },
  { href: '/budgets', label: 'Budgets' },
] as const;

/**
 * Spentra geometric prism logo SVG.
 */
function SpentraLogo() {
  return (
    <svg
      width="32"
      height="32"
      viewBox="0 0 32 32"
      fill="none"
      className="text-on-surface"
      aria-hidden="true"
    >
      <path
        d="M16 4L4 10V22L16 28L28 22V10L16 4Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2.5"
      />
      <path
        d="M16 4V28"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2.5"
      />
      <path
        d="M4 10L16 16L28 10"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2.5"
      />
    </svg>
  );
}

/**
 * Navbar — the fixed top navigation bar for the Spentra application.
 *
 * Features a glass-morphism backdrop, the geometric Spentra logo,
 * desktop nav links with active state indicators, and right-side
 * controls (theme toggle, new entry CTA, avatar).
 */
export default function Navbar({ onNewEntry }: NavbarProps) {
  const pathname = usePathname();
  const { user } = useAuth();

  return (
    <header className="fixed top-0 w-full z-50 bg-surface/30 backdrop-blur-[24px] backdrop-saturate-[180%] border-b border-outline-variant/10 shadow-[0_4px_30px_rgba(0,0,0,0.03)]">
      <nav className="max-w-7xl mx-auto h-16 px-6 flex items-center justify-between">
        {/* ── Left: Logo + Nav Links ── */}
        <div className="flex items-center gap-8">
          {/* Logo */}
          <Link href="/dashboard" className="flex items-center gap-2.5 group">
            <SpentraLogo />
            <span className="text-xl font-black tracking-tighter text-on-surface">
              Spentra
            </span>
          </Link>

          {/* Desktop nav links — hidden on mobile */}
          <div className="hidden md:flex items-center gap-6">
            {NAV_LINKS.map((link) => {
              const isActive = pathname === link.href;

              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={[
                    'text-sm transition-colors duration-200 py-1',
                    isActive
                      ? 'font-semibold text-on-surface border-b-2 border-on-surface'
                      : 'text-on-surface-variant hover:text-on-surface',
                  ].join(' ')}
                >
                  {link.label}
                </Link>
              );
            })}
          </div>
        </div>

        {/* ── Right: Controls ── */}
        <div className="flex items-center gap-3">
          {/* Theme toggle */}
          <ThemeToggle />

          {/* New Entry CTA */}
          <button
            onClick={onNewEntry}
            className={[
              'void-gradient text-on-primary cursor-pointer',
              'text-xs uppercase tracking-widest font-bold',
              'px-4 py-2 rounded-xl',
              'inline-flex items-center gap-2',
              'transition-all duration-200 active:scale-95',
              'shadow-sm hover:shadow-md',
            ].join(' ')}
            type="button"
          >
            <Plus className="w-4 h-4" />
            <span className="hidden sm:inline">New Entry</span>
          </button>

          {/* User avatar linking to settings */}
          <Link href="/settings" className="block active:scale-95 transition-transform" aria-label="Account Settings">
            <Avatar name={user?.name || user?.email || 'U'} size="sm" />
          </Link>
        </div>
      </nav>
    </header>
  );
}
