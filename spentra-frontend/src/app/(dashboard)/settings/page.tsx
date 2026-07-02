/**
 * @file Settings page — user profile and account management.
 */

'use client';

import { useState } from 'react';
import { useAuth } from '@/providers/AuthProvider';
import { useSettings, type CurrencyCode } from '@/providers/SettingsProvider';
import { useRouter } from 'next/navigation';
import { LogOut, User, Mail, Shield, Globe, Tags } from 'lucide-react';
import Button from '@/components/Button';
import Avatar from '@/components/Avatar';
import ManageCategoriesModal from '@/features/categories/ManageCategoriesModal';

export default function SettingsPage() {
  const { user, logout } = useAuth();
  const { currency, setCurrency } = useSettings();
  const router = useRouter();
  const [showCategoriesModal, setShowCategoriesModal] = useState(false);

  function handleLogout() {
    logout();
    router.push('/login');
  }

  if (!user) return null;

  return (
    <div className="animate-fade-in max-w-2xl mx-auto">
      <header className="mb-10">
        <h1 className="text-4xl font-bold tracking-tight text-on-surface mb-2">
          Account Settings
        </h1>
        <p className="text-sm text-on-surface-variant">
          Manage your profile and application preferences.
        </p>
      </header>

      {/* Profile Card */}
      <section className="bg-surface-container-lowest p-8 rounded-[1.5rem] shadow-sm mb-8">
        <div className="flex items-center gap-6 mb-8">
          <Avatar name={user.name || user.email} size="lg" />
          <div>
            <h2 className="text-xl font-bold text-on-surface">{user.name || 'Spentra User'}</h2>
            <p className="text-sm text-on-surface-variant flex items-center gap-2 mt-1">
              <Mail className="w-4 h-4" /> {user.email}
            </p>
          </div>
        </div>

        <div className="space-y-4 border-t border-outline-variant/20 pt-6">
          <div className="flex items-center justify-between py-2">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-surface-container rounded-lg text-on-surface-variant">
                <User className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-on-surface">Personal Information</p>
                <p className="text-xs text-on-surface-variant">Update your name and email</p>
              </div>
            </div>
            <Button variant="secondary" size="sm" disabled>Edit</Button>
          </div>

          <div className="flex items-center justify-between py-2">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-surface-container rounded-lg text-on-surface-variant">
                <Shield className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-on-surface">Security</p>
                <p className="text-xs text-on-surface-variant">Change password and security settings</p>
              </div>
            </div>
            <Button variant="secondary" size="sm" disabled>Update</Button>
          </div>
        </div>
      </section>

      {/* Preferences */}
      <section className="bg-surface-container-lowest p-8 rounded-[1.5rem] shadow-sm mb-8">
        <h3 className="text-lg font-bold text-on-surface mb-4">Preferences</h3>
        <div className="space-y-4 border-t border-outline-variant/20 pt-6">
          <div className="flex items-center justify-between py-2">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-surface-container rounded-lg text-on-surface-variant">
                <Globe className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-on-surface">Currency</p>
                <p className="text-xs text-on-surface-variant">Display currency across the app</p>
              </div>
            </div>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value as CurrencyCode)}
              className="bg-surface-container-low text-on-surface text-sm rounded-lg px-3 py-2 border-none outline-none focus:ring-2 focus:ring-tertiary cursor-pointer"
            >
              <option value="INR">Indian Rupee (₹)</option>
              <option value="USD">US Dollar ($)</option>
              <option value="EUR">Euro (€)</option>
              <option value="GBP">British Pound (£)</option>
            </select>
          </div>

          <div className="flex items-center justify-between py-2 border-t border-outline-variant/10 pt-4 mt-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-surface-container rounded-lg text-on-surface-variant">
                <Tags className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-on-surface">Categories</p>
                <p className="text-xs text-on-surface-variant">Manage custom spending categories</p>
              </div>
            </div>
            <Button variant="secondary" size="sm" onClick={() => setShowCategoriesModal(true)}>
              Manage
            </Button>
          </div>
        </div>
      </section>

      {/* Danger Zone */}
      <section className="bg-error-container/10 p-8 rounded-[1.5rem] border border-error-container/20">
        <h3 className="text-lg font-bold text-on-surface mb-4">Account Actions</h3>
        <p className="text-sm text-on-surface-variant mb-6">
          Logging out will clear your session on this device.
        </p>
        <Button variant="danger" icon={<LogOut className="w-4 h-4" />} onClick={handleLogout}>
          Sign Out
        </Button>
      </section>

      <ManageCategoriesModal
        isOpen={showCategoriesModal}
        onClose={() => setShowCategoriesModal(false)}
      />
    </div>
  );
}
