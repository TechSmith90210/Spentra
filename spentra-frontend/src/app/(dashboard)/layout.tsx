/**
 * @file Dashboard layout — authenticated shell with Navbar and MobileNav.
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/providers/AuthProvider';
import Navbar from '@/components/Navbar';
import MobileNav from '@/components/MobileNav';
import AddTransactionModal from '@/features/transactions/AddTransactionModal';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [showAddModal, setShowAddModal] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isLoading, isAuthenticated, router]);

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface">
        <div className="flex flex-col items-center gap-4">
          <svg width="48" height="48" viewBox="0 0 32 32" fill="none" className="text-on-surface animate-pulse">
            <path d="M16 4L4 10V22L16 28L28 22V10L16 4Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
            <path d="M16 4V28" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
            <path d="M4 10L16 16L28 10" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
          </svg>
          <p className="text-sm text-on-surface-variant">Loading...</p>
        </div>
      </div>
    );
  }

  // Not authenticated
  if (!isAuthenticated) return null;

  return (
    <div className="min-h-screen bg-surface">
      <Navbar onNewEntry={() => setShowAddModal(true)} />
      <main className="pt-20 pb-24 md:pb-8 px-4 md:px-6 max-w-7xl mx-auto min-h-screen">
        {children}
      </main>
      <MobileNav />
      <AddTransactionModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSuccess={() => {
          setShowAddModal(false);
          // Force refresh data by reloading current page
          router.refresh();
          // Dispatch global event for client components to refetch
          window.dispatchEvent(new CustomEvent('spentra-refresh-data'));
        }}
      />
    </div>
  );
}
