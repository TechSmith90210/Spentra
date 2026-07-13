/**
 * @file Settings page — user profile and account management.
 */

'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/providers/AuthProvider';
import { useSettings, type CurrencyCode } from '@/providers/SettingsProvider';
import { useRouter } from 'next/navigation';
import { LogOut, User, Mail, Shield, Globe, Tags, Camera, Check, Link as LinkIcon } from 'lucide-react';
import Button from '@/components/Button';
import Avatar from '@/components/Avatar';
import Input from '@/components/Input';
import Modal from '@/components/Modal';
import ManageCategoriesModal from '@/features/categories/ManageCategoriesModal';
import { updateProfile } from '@/lib/api/users';

const PRESET_AVATARS = [
  { name: 'Felix', url: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Felix' },
  { name: 'Aneka', url: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Aneka' },
  { name: 'Jack', url: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Jack' },
  { name: 'Buster', url: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Buster' },
  { name: 'Coco', url: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Coco' },
  { name: 'Mia', url: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Mia' },
];

export default function SettingsPage() {
  const { user, logout, updateUser } = useAuth();
  const { currency, setCurrency } = useSettings();
  const router = useRouter();
  const [showCategoriesModal, setShowCategoriesModal] = useState(false);
  const [showEditProfileModal, setShowEditProfileModal] = useState(false);

  // Edit Profile form state
  const [editName, setEditName] = useState('');
  const [editProfilePic, setEditProfilePic] = useState('');
  const [isCustomUrl, setIsCustomUrl] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // Hydrate form state when opening the edit modal
  useEffect(() => {
    if (user && showEditProfileModal) {
      setEditName(user.name || '');
      setEditProfilePic(user.profilePic || '');

      // Determine if currently selected avatar is custom or preset
      const isPreset = PRESET_AVATARS.some((p) => p.url === user.profilePic);
      setIsCustomUrl(!!user.profilePic && !isPreset);
      setError('');
    }
  }, [user, showEditProfileModal]);

  function handleLogout() {
    logout();
    router.push('/login');
  }

  async function handleSaveProfile(e: React.FormEvent) {
    e.preventDefault();
    if (!editName.trim()) {
      setError('Name is required');
      return;
    }

    try {
      setSaving(true);
      setError('');

      const finalProfilePic = editProfilePic.trim() || undefined;

      await updateProfile({
        name: editName.trim(),
        profilePic: finalProfilePic,
      });

      // Update AuthProvider state
      updateUser(editName.trim(), finalProfilePic);
      setShowEditProfileModal(false);
    } catch (err: any) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
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
          <Avatar name={user.name || user.email} src={user.profilePic} size="lg" className="w-16 h-16" />
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
                <p className="text-xs text-on-surface-variant">Update your name and profile picture</p>
              </div>
            </div>
            <Button variant="secondary" size="sm" onClick={() => setShowEditProfileModal(true)}>Edit</Button>
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

      {/* Manage Categories Modal */}
      <ManageCategoriesModal
        isOpen={showCategoriesModal}
        onClose={() => setShowCategoriesModal(false)}
      />

      {/* Edit Profile Modal */}
      <Modal
        isOpen={showEditProfileModal}
        onClose={() => setShowEditProfileModal(false)}
        title="Edit Profile"
      >
        <form onSubmit={handleSaveProfile} className="space-y-6">
          {error && (
            <div className="px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
              {error}
            </div>
          )}

          <Input
            label="Full Name"
            placeholder="Your display name"
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            required
            icon={<User className="w-4 h-4" />}
          />

          {/* Profile Picture Selector */}
          <div>
            <label className="block text-xs uppercase tracking-widest text-on-surface-variant mb-3 font-medium">
              Profile Picture
            </label>

            <div className="flex gap-4 items-center mb-4">
              <Avatar
                name={editName || user.email}
                src={editProfilePic}
                size="lg"
                className="w-16 h-16 ring-2 ring-tertiary/20"
              />
              <div className="text-xs text-on-surface-variant">
                Choose a preset character avatar or enter a custom web image URL below.
              </div>
            </div>

            {/* Presets */}
            <div className="mb-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-on-surface-variant">Preset Avatars</span>
                <button
                  type="button"
                  onClick={() => {
                    setIsCustomUrl(!isCustomUrl);
                    if (isCustomUrl) {
                      setEditProfilePic('');
                    }
                  }}
                  className="text-xs font-semibold text-tertiary hover:underline flex items-center gap-1 cursor-pointer"
                >
                  <LinkIcon className="w-3 h-3" />
                  {isCustomUrl ? 'Use Presets' : 'Use Custom URL'}
                </button>
              </div>

              {!isCustomUrl ? (
                <div className="grid grid-cols-6 gap-2">
                  {PRESET_AVATARS.map((avatar) => {
                    const isSelected = editProfilePic === avatar.url;
                    return (
                      <button
                        key={avatar.name}
                        type="button"
                        onClick={() => setEditProfilePic(avatar.url)}
                        className={`relative rounded-xl p-1 bg-surface-container-low border-2 transition-all cursor-pointer hover:scale-105 ${
                          isSelected ? 'border-tertiary bg-tertiary/5' : 'border-transparent'
                        }`}
                      >
                        <img src={avatar.url} alt={avatar.name} className="w-12 h-12 object-contain" />
                        {isSelected && (
                          <span className="absolute bottom-1 right-1 bg-tertiary text-on-tertiary p-0.5 rounded-full">
                            <Check className="w-2.5 h-2.5" />
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              ) : (
                <Input
                  placeholder="https://example.com/avatar.png"
                  value={editProfilePic}
                  onChange={(e) => setEditProfilePic(e.target.value)}
                  icon={<Camera className="w-4 h-4" />}
                />
              )}
            </div>
          </div>

          <div className="flex gap-3 justify-end pt-4 border-t border-outline-variant/10">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setShowEditProfileModal(false)}
              disabled={saving}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={saving}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
