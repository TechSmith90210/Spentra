'use client';

import { useState, useEffect } from 'react';
import Modal from '@/components/Modal';
import Input from '@/components/Input';
import Button from '@/components/Button';
import { getCategories, createCategory, deleteCategory } from '@/lib/api/categories';
import type { Category } from '@/lib/api/types';
import { Trash2, Plus } from 'lucide-react';
import * as LucideIcons from 'lucide-react';
import { getCategoryIcon } from '@/lib/utils';

interface ManageCategoriesModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const toPascalCase = (str: string) =>
  str
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join('');

export default function ManageCategoriesModal({ isOpen, onClose }: ManageCategoriesModalProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  const fetchCategories = async () => {
    try {
      setLoading(true);
      const data = await getCategories();
      setCategories(data);
    } catch (err: any) {
      setError('Failed to load categories');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchCategories();
    }
  }, [isOpen]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;

    try {
      setCreating(true);
      setError('');
      await createCategory({ name: newCategoryName.trim() });
      setNewCategoryName('');
      await fetchCategories();
      window.dispatchEvent(new CustomEvent('spentra-refresh-data'));
    } catch (err: any) {
      setError(err.message || 'Failed to create category');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this category? Related transactions might lose their category reference.')) return;
    try {
      setError('');
      await deleteCategory(id);
      await fetchCategories();
      window.dispatchEvent(new CustomEvent('spentra-refresh-data'));
    } catch (err: any) {
      setError(err.message || 'Failed to delete category');
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Manage Categories">
      {error && (
        <div className="mb-4 px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
          {error}
        </div>
      )}

      {/* Add New */}
      <form onSubmit={handleCreate} className="flex gap-2 mb-6">
        <div className="flex-1">
          <Input
            label="New Category"
            placeholder="e.g. Subscriptions"
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
            name="newCategoryName"
            required
          />
        </div>
        <div className="pt-6">
          <Button type="submit" variant="primary" loading={creating} disabled={!newCategoryName.trim()}>
            <Plus className="w-5 h-5" />
          </Button>
        </div>
      </form>

      {/* List */}
      <div className="space-y-2 max-h-[400px] overflow-y-auto pr-2">
        {loading ? (
          <div className="text-center py-4 text-on-surface-variant text-sm">Loading...</div>
        ) : categories.length === 0 ? (
          <div className="text-center py-4 text-on-surface-variant text-sm">No categories found.</div>
        ) : (
          categories.map((category) => {
            const iconNameKebab = getCategoryIcon(category.name);
            const iconNamePascal = toPascalCase(iconNameKebab) as keyof typeof LucideIcons;
            const IconComponent = (LucideIcons[iconNamePascal] || LucideIcons.CircleHelp) as React.ElementType;

            return (
              <div key={category.id} className="flex items-center justify-between p-3 rounded-xl bg-surface-container-lowest border border-outline-variant/10">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-secondary-container text-on-secondary-container rounded-lg">
                    <IconComponent className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-on-surface">{category.name}</span>
                </div>
                <button
                  type="button"
                  onClick={() => handleDelete(category.id)}
                  className="p-2 text-on-surface-variant hover:text-error hover:bg-error-container/20 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            );
          })
        )}
      </div>
    </Modal>
  );
}
