/**
 * @file Root layout for the Spentra application.
 *
 * Configures Space Grotesk as the primary font, wraps the app in
 * Auth and Theme providers, and establishes the base HTML structure.
 */

import type { Metadata } from 'next';
import { Space_Grotesk } from 'next/font/google';
import { Analytics } from '@vercel/analytics/next';
import { AuthProvider } from '@/providers/AuthProvider';
import { ThemeProvider } from '@/providers/ThemeProvider';
import { SettingsProvider } from '@/providers/SettingsProvider';
import './globals.css';

const spaceGrotesk = Space_Grotesk({
  subsets: ['latin'],
  variable: '--font-space-grotesk',
});

export const metadata: Metadata = {
  title: 'Spentra — Precision Expense Tracking',
  description:
    'Track expenses, manage budgets, and gain financial clarity with Spentra.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${spaceGrotesk.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col font-sans">
        <ThemeProvider>
          <SettingsProvider>
            <AuthProvider>{children}</AuthProvider>
          </SettingsProvider>
        </ThemeProvider>
        <Analytics />
      </body>
    </html>
  );
}
