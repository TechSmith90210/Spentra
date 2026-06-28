/**
 * @file Auth layout — centered card layout for login/signup pages.
 */

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-surface relative overflow-hidden">
      {/* Subtle decorative elements */}
      <div className="absolute top-0 right-0 w-96 h-96 bg-tertiary/5 rounded-full blur-3xl -mr-48 -mt-48" />
      <div className="absolute bottom-0 left-0 w-72 h-72 bg-secondary-container/20 rounded-full blur-3xl -ml-36 -mb-36" />
      <div className="relative z-10 w-full max-w-md px-6 py-12">
        {children}
      </div>
    </div>
  );
}
