/**
 * @file Login page — email/password authentication.
 */

'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Mail, Lock } from 'lucide-react';
import { useAuth } from '@/providers/AuthProvider';
import Button from '@/components/Button';
import Input from '@/components/Input';
import Link from 'next/link';

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ email, password });
      router.push('/dashboard');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Login failed. Please try again.';
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
      <h1 className="text-3xl font-bold tracking-tight text-on-surface mb-2">Welcome back</h1>
      <p className="text-sm text-on-surface-variant mb-8">Sign in to your account to continue</p>

      {/* Error */}
      {error && (
        <div className="mb-6 px-4 py-3 bg-error-container/20 text-on-error-container rounded-xl text-sm">
          {error}
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-5">
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
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          icon={<Lock className="w-4 h-4" />}
          required
          name="password"
        />
        <Button type="submit" variant="primary" fullWidth loading={loading} size="lg">
          Sign In
        </Button>
      </form>

      {/* Footer */}
      <p className="mt-8 text-center text-sm text-on-surface-variant">
        Don&apos;t have an account?{' '}
        <Link href="/signup" className="text-tertiary font-semibold hover:underline">
          Create one
        </Link>
      </p>
    </div>
  );
}
