/**
 * @file Signup page — new account registration.
 */

'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Mail, Lock, User } from 'lucide-react';
import { useAuth } from '@/providers/AuthProvider';
import Button from '@/components/Button';
import Input from '@/components/Input';
import Link from 'next/link';

export default function SignUpPage() {
  const router = useRouter();
  const { signup } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    // Client-side validation
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (!/[a-zA-Z]/.test(password) || !/\d/.test(password)) {
      setError('Password must contain at least one letter and one number.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await signup({ email, password, confirmPassword, name: name || undefined });
      router.push('/dashboard');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Registration failed. Please try again.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="animate-fade-in">
      {/* Logo */}
      <div className="flex items-center gap-3 mb-10">
        <svg width="40" height="40" viewBox="0 0 32 32" fill="none" className="text-on-surface">
          <path d="M16 4L4 10V22L16 28L28 22V10L16 4Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
          <path d="M16 4V28" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
          <path d="M4 10L16 16L28 10" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
        </svg>
        <span className="text-2xl font-black tracking-tighter text-on-surface">Spentra</span>
      </div>

      {/* Heading */}
      <h1 className="text-3xl font-bold tracking-tight text-on-surface mb-2">Create account</h1>
      <p className="text-sm text-on-surface-variant mb-8">Start tracking your finances with precision</p>

      {/* Error */}
      {error && (
        <div className="mb-6 px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
          {error}
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-5">
        <Input
          label="Name"
          type="text"
          placeholder="Jane Doe"
          value={name}
          onChange={(e) => setName(e.target.value)}
          icon={<User className="w-4 h-4" />}
          name="name"
        />
        <Input
          label="Email"
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          icon={<Mail className="w-4 h-4" />}
          required
          name="email"
        />
        <Input
          label="Password"
          type="password"
          placeholder="Min. 8 characters"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          icon={<Lock className="w-4 h-4" />}
          required
          name="password"
        />
        <Input
          label="Confirm Password"
          type="password"
          placeholder="••••••••"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          icon={<Lock className="w-4 h-4" />}
          required
          name="confirmPassword"
        />
        <Button type="submit" fullWidth loading={loading}>
          Create Account
        </Button>

        {loading && (
          <p className="mt-4 text-xs text-center text-on-surface-variant animate-pulse">
            Setting up your account... (Our server may take up to 50 seconds to wake up if inactive)
          </p>
        )}
      </form>

      {/* Footer */}
      <p className="mt-8 text-center text-sm text-on-surface-variant">
        Already have an account?{' '}
        <Link href="/login" className="text-tertiary font-semibold hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
