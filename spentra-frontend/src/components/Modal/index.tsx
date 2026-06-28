/**
 * @file Modal/index.tsx
 * @description Spentra design system Modal component.
 *
 * A centered dialog with backdrop blur, body scroll lock, and
 * click-outside-to-close behavior. Follows the editorial surface
 * hierarchy with surface-container-lowest panels and 1.5rem corners.
 *
 * @example
 * ```tsx
 * <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Add Expense">
 *   <form>...</form>
 * </Modal>
 * ```
 */

'use client';

import { useEffect, useCallback, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';

/** Size preset for the modal panel */
type ModalSize = 'sm' | 'md' | 'lg';

/** Props for the Modal component */
export interface ModalProps {
  /** Whether the modal is visible */
  isOpen: boolean;
  /** Callback fired when the modal should close */
  onClose: () => void;
  /** Modal header title */
  title?: string;
  /** Modal body content */
  children: ReactNode;
  /** Width preset for the modal panel */
  size?: ModalSize;
}

/** Max-width classes for each size preset */
const sizeStyles: Record<ModalSize, string> = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
};

/**
 * Modal — a centered overlay dialog for the Spentra UI.
 *
 * Features a blurred backdrop, CSS scale/opacity transitions,
 * body scroll locking, click-outside dismiss, and Escape key support.
 */
export default function Modal({
  isOpen,
  onClose,
  title,
  children,
  size = 'md',
}: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  /** Lock body scroll when the modal is open */
  useEffect(() => {
    if (isOpen) {
      const scrollY = window.scrollY;
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';

      return () => {
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        document.body.style.overflow = '';
        window.scrollTo(0, scrollY);
      };
    }
  }, [isOpen]);

  /** Close on Escape key */
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose]
  );

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      return () => document.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, handleKeyDown]);

  /** Close when clicking the backdrop (outside panel) */
  const handleBackdropClick = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        onClose();
      }
    },
    [onClose]
  );

  if (!isOpen) return null;

  return (
    <div
      className={[
        'fixed inset-0 z-[100]',
        'flex items-end sm:items-center justify-center',
        'bg-black/50 backdrop-blur-sm',
        'p-0 sm:p-4',
        // Entrance animation
        'animate-in fade-in duration-200',
      ].join(' ')}
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-label={title}
    >
      {/* Panel */}
      <div
        ref={panelRef}
        className={[
          'w-full',
          sizeStyles[size],
          'bg-surface-container-lowest',
          'rounded-t-[2rem] sm:rounded-[1.5rem] shadow-2xl',
          'p-6 pb-[max(1.5rem,env(safe-area-inset-bottom))] sm:pb-6',
          // Animations
          'animate-slide-up sm:animate-scale-in',
        ].join(' ')}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          {title && (
              <h2 className="text-xl font-bold tracking-tight text-on-surface">
                {title}
              </h2>
            )}
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg hover:bg-surface-container-high transition-colors text-on-surface-variant hover:text-on-surface cursor-pointer"
              aria-label="Close modal"
              type="button"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

        {/* Body */}
        <div>{children}</div>
      </div>
    </div>
  );
}
