/**
 * @file Spentra Landing Page — Beautiful, clean, and user-friendly landing page for visitors.
 *
 * Features a high-contrast hero section with glowing ASCII art,
 * an interactive Terminal Sandbox to try Spentra live with real-time visual charting,
 * a clean bento grid of core benefits, and smooth transitions.
 */

'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Terminal,
  ArrowUpRight,
  ArrowDownRight,
  Sparkles,
  Plus,
  Coffee,
  Car,
  Utensils,
  Lock,
  Shield,
  Activity,
  Calendar,
  DollarSign,
  Briefcase,
  Layers,
  ChevronRight,
  ArrowRight
} from 'lucide-react';
import { useAuth } from '@/providers/AuthProvider';
import Button from '@/components/Button';
import ThemeToggle from '@/components/ThemeToggle';
import { formatCurrency } from '@/lib/utils';
import { useSettings } from '@/providers/SettingsProvider';

/** Geometric 3D isometric ASCII wireframe logo */
const ASCII_LOGO = `
       ._____________________.
      /                     /|
     /     S P E N T R A   / |
    /                     /  |
   +---------------------+   |
   |                     |   |
   |   P O R T F O L I O |   |
   |                     |  /
   |                     | /
   +---------------------+
`;

interface MockTransaction {
  id: string;
  title: string;
  amount: number;
  type: 'CREDIT' | 'EXPENSE';
  category: 'salary' | 'coffee' | 'transport' | 'food' | 'entertainment';
  date: string;
}

export default function LandingPage() {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const { currency } = useSettings();

  // Redirect authenticated users to the dashboard
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isLoading, isAuthenticated, router]);

  // Terminal Sandbox State
  const [sandboxTxs, setSandboxTxs] = useState<MockTransaction[]>([
    { id: '1', title: 'Salary Injected', amount: 1200, type: 'CREDIT', category: 'salary', date: 'Jul 15' },
    { id: '2', title: 'Monthly Rent', amount: 450, type: 'EXPENSE', category: 'food', date: 'Jul 16' },
    { id: '3', title: 'Specialty Espresso', amount: 15, type: 'EXPENSE', category: 'coffee', date: 'Jul 17' },
  ]);

  const [inputTitle, setInputTitle] = useState('');
  const [inputAmount, setInputAmount] = useState('');
  const [inputType, setInputType] = useState<'CREDIT' | 'EXPENSE'>('EXPENSE');
  const [inputCategory, setInputCategory] = useState<MockTransaction['category']>('food');
  const [consoleLogs, setConsoleLogs] = useState<string[]>([
    'SYSTEM: Spentra Sandbox initialized.',
    'LEDGER: Loaded 3 demo transactions successfully.',
    'SANDBOX: Interactive terminal is online.'
  ]);

  // Terminal actions
  const logMessage = (msg: string) => {
    setConsoleLogs(prev => [...prev.slice(-4), msg]); // Keep last 5 lines
  };

  const handleInjectPreset = (preset: { title: string; amount: number; type: 'CREDIT' | 'EXPENSE'; category: MockTransaction['category'] }) => {
    const newTx: MockTransaction = {
      id: Math.random().toString(),
      title: preset.title,
      amount: preset.amount,
      type: preset.type,
      category: preset.category,
      date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    };
    setSandboxTxs(prev => [...prev, newTx]);
    logMessage(`LOG: Added ${newTx.type} "${newTx.title}" of ${formatCurrency(newTx.amount, currency)}.`);
  };

  const handleInjectCustom = (e: React.FormEvent) => {
    e.preventDefault();
    const amt = parseFloat(inputAmount);
    if (!inputTitle || isNaN(amt) || amt <= 0) {
      logMessage('ERROR: Invalid name or amount specified.');
      return;
    }
    const newTx: MockTransaction = {
      id: Math.random().toString(),
      title: inputTitle,
      amount: amt,
      type: inputType,
      category: inputCategory,
      date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    };
    setSandboxTxs(prev => [...prev, newTx]);
    logMessage(`LOG: Added ${newTx.type} "${newTx.title}" of ${formatCurrency(newTx.amount, currency)}.`);
    setInputTitle('');
    setInputAmount('');
  };

  const handleResetLedger = () => {
    setSandboxTxs([
      { id: '1', title: 'Salary Injected', amount: 1200, type: 'CREDIT', category: 'salary', date: 'Jul 15' },
      { id: '2', title: 'Monthly Rent', amount: 450, type: 'EXPENSE', category: 'food', date: 'Jul 16' },
      { id: '3', title: 'Specialty Espresso', amount: 15, type: 'EXPENSE', category: 'coffee', date: 'Jul 17' },
    ]);
    setConsoleLogs([
      'SYSTEM: Sandbox memory reset.',
      'LEDGER: Restored 3 basic demo entries.'
    ]);
  };

  // Computations
  const totals = useMemo(() => {
    let balance = 0;
    let credit = 0;
    let expense = 0;
    sandboxTxs.forEach(t => {
      if (t.type === 'CREDIT') {
        balance += t.amount;
        credit += t.amount;
      } else {
        balance -= t.amount;
        expense += t.amount;
      }
    });
    return { balance, credit, expense };
  }, [sandboxTxs]);

  // Compute interactive ASCII block chart plotting running balance
  const runningBalances = useMemo(() => {
    let bal = 0;
    return sandboxTxs.map(tx => {
      if (tx.type === 'CREDIT') bal += tx.amount;
      else bal -= tx.amount;
      return bal;
    });
  }, [sandboxTxs]);

  const asciiChart = useMemo(() => {
    if (runningBalances.length === 0) return 'No entries to plot.';
    // Take up to last 6 entries to keep chart tidy
    const data = runningBalances.slice(-6);
    const minVal = Math.min(...data, 0);
    const maxVal = Math.max(...data, 200);
    const range = maxVal - minVal || 1;
    const rows = 5;
    const cols = data.length;

    let chartLines: string[] = [];
    for (let r = rows - 1; r >= 0; r--) {
      const threshold = minVal + (range / (rows - 1)) * r;
      let line = `${String(Math.round(threshold)).padStart(5)} | `;
      for (let c = 0; c < cols; c++) {
        const val = data[c];
        if (val >= threshold && val !== 0) {
          line += '  ████  ';
        } else {
          line += '        ';
        }
      }
      chartLines.push(line);
    }
    chartLines.push('      +' + '--------'.repeat(cols));
    // Print indexes matching last 6 transactions
    let labelLine = '        ';
    const startIdx = Math.max(0, runningBalances.length - 6);
    for (let c = 0; c < cols; c++) {
      labelLine += `   T${startIdx + c + 1}   `;
    }
    chartLines.push(labelLine);
    return chartLines.join('\n');
  }, [runningBalances]);

  // Render Category Mini Icon helper
  const getCategoryIcon = (cat: MockTransaction['category']) => {
    switch (cat) {
      case 'salary': return <Briefcase className="w-3.5 h-3.5 text-income" />;
      case 'coffee': return <Coffee className="w-3.5 h-3.5 text-tertiary" />;
      case 'transport': return <Car className="w-3.5 h-3.5 text-secondary" />;
      case 'food': return <Utensils className="w-3.5 h-3.5 text-error" />;
      default: return <Layers className="w-3.5 h-3.5" />;
    }
  };

  // Loading/Redirect Shell
  if (isLoading || isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface">
        <div className="flex flex-col items-center gap-6 animate-pulse">
          <svg width="64" height="64" viewBox="0 0 32 32" fill="none" className="text-tertiary">
            <path d="M16 4L4 10V22L16 28L28 22V10L16 4Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
            <path d="M16 4V28" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
            <path d="M4 10L16 16L28 10" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
          </svg>
          <div className="text-center space-y-1">
            <p className="text-sm font-bold tracking-widest uppercase text-on-surface">Initializing Spentra</p>
            <p className="text-xs text-on-surface-variant font-mono">loading your secure environment...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface selection:bg-tertiary/10 text-on-surface flex flex-col relative overflow-hidden">
      {/* Decorative Blur Background Elements */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-tertiary/5 rounded-full blur-3xl -mr-64 -mt-64 pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-secondary/10 rounded-full blur-3xl -ml-48 -mb-48 pointer-events-none" />

      {/* FIXED LANDING NAVBAR */}
      <header className="fixed top-0 w-full z-50 bg-surface/30 backdrop-blur-[24px] backdrop-saturate-[180%] border-b border-outline-variant/10 shadow-[0_4px_30px_rgba(0,0,0,0.03)]">
        <nav className="max-w-7xl mx-auto h-16 px-6 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2.5">
            <svg width="28" height="28" viewBox="0 0 32 32" fill="none" className="text-on-surface">
              <path d="M16 4L4 10V22L16 28L28 22V10L16 4Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
              <path d="M16 4V28" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
              <path d="M4 10L16 16L28 10" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
            </svg>
            <span className="text-lg font-black tracking-tighter text-on-surface">Spentra</span>
          </Link>

          <div className="flex items-center gap-4">
            <ThemeToggle />
            <Link href="/login">
              <Button variant="secondary" size="sm">
                Sign In
              </Button>
            </Link>
            <Link href="/signup">
              <Button variant="primary" size="sm">
                Get Started
              </Button>
            </Link>
          </div>
        </nav>
      </header>

      {/* HERO SECTION */}
      <section className="pt-32 pb-24 px-6 max-w-7xl mx-auto w-full grid grid-cols-1 lg:grid-cols-12 gap-12 items-center relative z-10">
        {/* Left copy */}
        <div className="lg:col-span-7 space-y-6 animate-slide-up">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-surface-container-high text-on-surface-variant rounded-full text-xs font-bold uppercase tracking-wider">
            <Sparkles className="w-3.5 h-3.5 text-tertiary" />
            <span>Effortless Expense Tracking</span>
          </div>

          <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight leading-[1.05] text-on-surface">
            Financial Clarity, <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-tertiary to-tertiary-container">
              Simplified.
            </span>
          </h1>

          <p className="text-lg text-on-surface-variant max-w-xl leading-relaxed">
            Spentra is a clean, lightning-fast expense tracker. Focus on your daily money habits with beautiful real-time insights, automated budget alerts, and clean visual statistics to give you absolute control over your money.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 pt-4">
            <Link href="/signup">
              <Button variant="primary" size="lg" className="shadow-md">
                Start Tracking Free <ArrowRight className="w-4 h-4 ml-1" />
              </Button>
            </Link>
            <a href="#sandbox">
              <Button variant="secondary" size="lg">
                Try Sandbox Console
              </Button>
            </a>
          </div>
        </div>

        {/* Right graphic — Gorgeous isometric ASCII logo */}
        <div className="lg:col-span-5 flex justify-center items-center animate-fade-in delay-200">
          <div className="relative p-8 bg-surface-container-low rounded-[2rem] border border-outline-variant/10 shadow-lg group hover:scale-[1.02] transition-all duration-300">
            <div className="absolute inset-0 bg-gradient-to-tr from-tertiary/10 to-transparent rounded-[2rem] opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
            <pre className="text-tertiary font-mono text-[10px] sm:text-xs leading-tight select-none tracking-normal drop-shadow-[0_0_20px_rgba(112,115,255,0.2)]">
              {ASCII_LOGO}
            </pre>
            <div className="mt-6 flex justify-between items-center text-xs font-mono text-on-surface-variant/80 border-t border-outline-variant/15 pt-4">
              <span>STATUS: SYNCED</span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-income animate-pulse" />
                ONLINE
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* DYNAMIC SANDBOX TERMINAL SECTION */}
      <section id="sandbox" className="py-24 px-6 bg-surface-container-lowest/50 border-y border-outline-variant/10 relative z-10 scroll-mt-16">
        <div className="max-w-7xl mx-auto">
          <div className="text-center max-w-2xl mx-auto mb-16 space-y-4">
            <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-on-surface">
              Interactive Sandbox
            </h2>
            <p className="text-on-surface-variant leading-relaxed">
              Try Spentra live. Add mock income and expenses below to see how our interactive sandbox automatically updates your balance and plots a clean visual chart.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
            {/* Control Panel / Presets */}
            <div className="lg:col-span-4 flex flex-col justify-between bg-surface-container-low p-8 rounded-3xl border border-outline-variant/10">
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-on-surface mb-1">Quick Sandbox Simulator</h3>
                  <p className="text-xs text-on-surface-variant">Click on any action or add a custom entry to test the dashboard.</p>
                </div>

                {/* Preset Trigger buttons */}
                <div className="space-y-2.5">
                  <p className="text-xs uppercase font-bold tracking-wider text-on-surface-variant">Preset Actions</p>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => handleInjectPreset({ title: 'Bonus Injected', amount: 150, type: 'CREDIT', category: 'salary' })}
                      className="flex items-center gap-2 p-3 rounded-xl bg-surface hover:bg-surface-container-high border border-outline-variant/5 text-xs font-semibold text-income cursor-pointer active:scale-95 transition-all text-left"
                    >
                      <Plus className="w-3.5 h-3.5 shrink-0" />
                      <span>+ $150 Bonus</span>
                    </button>
                    <button
                      onClick={() => handleInjectPreset({ title: 'Uber Ride', amount: 25, type: 'EXPENSE', category: 'transport' })}
                      className="flex items-center gap-2 p-3 rounded-xl bg-surface hover:bg-surface-container-high border border-outline-variant/5 text-xs font-semibold text-error cursor-pointer active:scale-95 transition-all text-left"
                    >
                      <span>- $25 Uber</span>
                    </button>
                    <button
                      onClick={() => handleInjectPreset({ title: 'Burger Dinner', amount: 35, type: 'EXPENSE', category: 'food' })}
                      className="flex items-center gap-2 p-3 rounded-xl bg-surface hover:bg-surface-container-high border border-outline-variant/5 text-xs font-semibold text-error cursor-pointer active:scale-95 transition-all text-left"
                    >
                      <span>- $35 Dinner</span>
                    </button>
                    <button
                      onClick={() => handleInjectPreset({ title: 'Double Espresso', amount: 8, type: 'EXPENSE', category: 'coffee' })}
                      className="flex items-center gap-2 p-3 rounded-xl bg-surface hover:bg-surface-container-high border border-outline-variant/5 text-xs font-semibold text-error cursor-pointer active:scale-95 transition-all text-left"
                    >
                      <span>- $8 Coffee</span>
                    </button>
                  </div>
                </div>

                {/* Custom Transaction Injector Form */}
                <form onSubmit={handleInjectCustom} className="space-y-4 pt-4 border-t border-outline-variant/10">
                  <p className="text-xs uppercase font-bold tracking-wider text-on-surface-variant">Custom Entry</p>
                  <div className="space-y-3">
                    <input
                      type="text"
                      placeholder="Transaction Name"
                      value={inputTitle}
                      onChange={e => setInputTitle(e.target.value)}
                      className="w-full px-3.5 py-2.5 bg-surface text-xs rounded-xl border border-outline-variant/20 focus:outline-none focus:border-tertiary transition-colors"
                      required
                    />
                    <div className="flex gap-2">
                      <input
                        type="number"
                        placeholder="Amount"
                        value={inputAmount}
                        onChange={e => setInputAmount(e.target.value)}
                        className="flex-1 px-3.5 py-2.5 bg-surface text-xs rounded-xl border border-outline-variant/20 focus:outline-none focus:border-tertiary transition-colors"
                        required
                        min="1"
                      />
                      <select
                        value={inputType}
                        onChange={e => setInputType(e.target.value as 'CREDIT' | 'EXPENSE')}
                        className="px-3 py-2.5 bg-surface text-xs rounded-xl border border-outline-variant/20 focus:outline-none focus:border-tertiary transition-colors font-semibold"
                      >
                        <option value="EXPENSE">Expense</option>
                        <option value="CREDIT">Credit</option>
                      </select>
                    </div>
                  </div>
                  <button
                    type="submit"
                    className="w-full p-2.5 bg-tertiary hover:bg-tertiary-container text-on-tertiary text-xs font-bold rounded-xl active:scale-95 transition-all cursor-pointer shadow-sm text-center"
                  >
                    Add to Sandbox
                  </button>
                </form>
              </div>

              <button
                onClick={handleResetLedger}
                className="mt-6 w-full p-2 bg-surface-container-highest hover:bg-outline-variant/20 text-on-surface-variant text-[11px] font-bold uppercase tracking-wider rounded-xl transition-colors cursor-pointer text-center"
              >
                Reset Sandbox
              </button>
            </div>

            {/* Simulated Live Terminal */}
            <div className="lg:col-span-8 bg-surface-container-lowest border border-outline-variant/15 rounded-3xl p-6 shadow-md flex flex-col justify-between overflow-hidden relative">
              {/* Terminal header */}
              <div className="flex items-center justify-between border-b border-outline-variant/10 pb-4 mb-4">
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full bg-error" />
                  <span className="w-3 h-3 rounded-full bg-warning" />
                  <span className="w-3 h-3 rounded-full bg-income" />
                  <span className="ml-2 font-mono text-xs text-on-surface-variant flex items-center gap-1.5">
                    <Terminal className="w-3.5 h-3.5" />
                    ~/spentra/sandbox (main)*
                  </span>
                </div>
                <div className="text-[10px] font-mono text-on-surface-variant/75">
                  SANDBOX BALANCE
                </div>
              </div>

              {/* Balance Summary Header Inside Terminal */}
              <div className="grid grid-cols-3 gap-4 bg-surface-container-low/60 rounded-2xl p-4 mb-6 font-mono border border-outline-variant/5">
                <div>
                  <div className="text-[10px] text-on-surface-variant uppercase">Balance</div>
                  <div className={`text-lg font-bold ${totals.balance >= 0 ? 'text-income' : 'text-error'}`}>
                    {formatCurrency(totals.balance, currency)}
                  </div>
                </div>
                <div>
                  <div className="text-[10px] text-on-surface-variant uppercase">Total Credit</div>
                  <div className="text-lg font-bold text-income">
                    {formatCurrency(totals.credit, currency)}
                  </div>
                </div>
                <div>
                  <div className="text-[10px] text-on-surface-variant uppercase">Total Expense</div>
                  <div className="text-lg font-bold text-error">
                    {formatCurrency(totals.expense, currency)}
                  </div>
                </div>
              </div>

              {/* Terminal Output Layout Grid (ASCII Chart + Live Ledger list) */}
              <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start flex-grow">
                {/* ASCII Chart */}
                <div className="md:col-span-7 font-mono flex flex-col">
                  <div className="text-[11px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">
                    Your Spending Trend (ASCII)
                  </div>
                  <div className="bg-surface-dim/40 dark:bg-black/40 p-4 rounded-2xl border border-outline-variant/10 text-[10px] sm:text-xs overflow-x-auto text-tertiary select-none leading-relaxed">
                    <pre className="whitespace-pre font-mono">{asciiChart}</pre>
                  </div>
                </div>

                {/* Ledger Listing */}
                <div className="md:col-span-5 flex flex-col font-mono">
                  <div className="text-[11px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">
                    Sandbox History
                  </div>
                  <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                    {sandboxTxs.map((tx, idx) => (
                      <div
                        key={tx.id}
                        className="flex items-center justify-between p-2.5 bg-surface-container-low hover:bg-surface-container-high rounded-xl border border-outline-variant/5 text-xs transition-all animate-fade-in"
                        style={{ animationDelay: `${idx * 40}ms` }}
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <span className="shrink-0 p-1.5 bg-surface rounded-lg">
                            {getCategoryIcon(tx.category)}
                          </span>
                          <span className="truncate font-semibold text-on-surface text-[11px]">{tx.title}</span>
                        </div>
                        <span className={`text-[11px] font-bold shrink-0 ${tx.type === 'CREDIT' ? 'text-income' : 'text-error'}`}>
                          {tx.type === 'CREDIT' ? '+' : '-'}${tx.amount}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Terminal Logs (CRT console prompt feel) */}
              <div className="mt-6 pt-4 border-t border-outline-variant/10 font-mono text-[10px] sm:text-xs text-on-surface-variant space-y-1 select-none">
                {consoleLogs.map((log, i) => (
                  <div key={i} className="flex gap-2">
                    <span className="text-tertiary/70">{'>'}</span>
                    <span className="truncate">{log}</span>
                  </div>
                ))}
                <div className="flex items-center gap-1">
                  <span className="text-income font-bold">~/spentra/sandbox $</span>
                  <span className="animate-pulse bg-on-surface h-3.5 w-1.5 inline-block" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CORE BENEFITS GRID */}
      <section className="py-24 px-6 max-w-7xl mx-auto w-full relative z-10">
        <div className="text-center max-w-2xl mx-auto mb-20 space-y-4">
          <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-on-surface">
            Built for Seamless Simplicity
          </h2>
          <p className="text-on-surface-variant leading-relaxed">
            Spentra is built to be fast, beautiful, and completely secure. No clutter, just the features you need to master your personal finance.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* Card 1 */}
          <div className="bg-surface-container-low p-8 rounded-3xl border border-outline-variant/10 shadow-sm hover:scale-[1.01] hover:shadow-md transition-all duration-300 flex flex-col justify-between">
            <div className="space-y-4">
              <span className="inline-flex p-3 bg-tertiary/10 text-tertiary rounded-2xl">
                <Shield className="w-6 h-6" />
              </span>
              <h3 className="text-xl font-bold text-on-surface">
                Smart Categorization
              </h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">
                Your transactions are automatically categorized and styled. See exactly where your money goes with elegant progress bars, breakdown stats, and clear summaries.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-outline-variant/10 text-xs font-mono text-on-surface-variant">
              DESIGN: MINIMAL & CLEAN
            </div>
          </div>

          {/* Card 2 */}
          <div className="bg-surface-container-low p-8 rounded-3xl border border-outline-variant/10 shadow-sm hover:scale-[1.01] hover:shadow-md transition-all duration-300 flex flex-col justify-between">
            <div className="space-y-4">
              <span className="inline-flex p-3 bg-secondary-container/30 text-on-secondary-container rounded-2xl">
                <Calendar className="w-6 h-6" />
              </span>
              <h3 className="text-xl font-bold text-on-surface">
                Automated Alerts
              </h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">
                Our automated background assistant keeps track of your recurring bills and warns you if your budgets are running hot, so you are always ahead of your expenses.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-outline-variant/10 text-xs font-mono text-on-surface-variant">
              SCHEDULE: DAILY UPDATE
            </div>
          </div>

          {/* Card 3 */}
          <div className="bg-surface-container-low p-8 rounded-3xl border border-outline-variant/10 shadow-sm hover:scale-[1.01] hover:shadow-md transition-all duration-300 flex flex-col justify-between md:col-span-2 lg:col-span-1">
            <div className="space-y-4">
              <span className="inline-flex p-3 bg-error-container/20 text-on-error-container rounded-2xl">
                <Lock className="w-6 h-6" />
              </span>
              <h3 className="text-xl font-bold text-on-surface">
                Bank-Grade Privacy
              </h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">
                Your data belongs to you. Spentra uses state-of-the-art token encryption and sandboxed session storage to guarantee your personal financial privacy.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-outline-variant/10 text-xs font-mono text-on-surface-variant">
              SECURITY: SECURE & PRIVATE
            </div>
          </div>
        </div>
      </section>

      {/* CALL TO ACTION */}
      <section className="py-24 px-6 relative z-10">
        <div className="max-w-4xl mx-auto bg-gradient-to-br from-surface-container-high via-surface-container-highest to-surface-container-low rounded-[2.5rem] border border-outline-variant/15 p-8 md:p-16 text-center shadow-lg relative overflow-hidden">
          {/* subtle abstract wireframe backdrop */}
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-tertiary/5 via-transparent to-transparent opacity-100 pointer-events-none" />

          <div className="max-w-xl mx-auto space-y-6 relative z-10">
            <h2 className="text-4xl font-extrabold tracking-tight text-on-surface">
              Take control of your financial future.
            </h2>
            <p className="text-on-surface-variant text-base leading-relaxed">
              Sign up today to start tracking your daily habits, managing smart budgets, and building long-term wealth with ease.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
              <Link href="/signup">
                <Button variant="primary" size="lg" className="px-8 py-4 text-base font-bold">
                  Create Free Account
                </Button>
              </Link>
              <Link href="/login">
                <Button variant="secondary" size="lg" className="px-8 py-4 text-base font-bold">
                  Sign In to Workspace
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* EDITORIAL FOOTER */}
      <footer className="mt-auto border-t border-outline-variant/10 py-12 px-6 bg-surface-container-lowest/20 text-center relative z-10">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6 text-sm text-on-surface-variant">
          <div className="flex items-center gap-2 font-bold tracking-tight text-on-surface">
            <svg width="20" height="20" viewBox="0 0 32 32" fill="none" className="text-on-surface">
              <path d="M16 4L4 10V22L16 28L28 22V10L16 4Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
              <path d="M16 4V28" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
              <path d="M4 10L16 16L28 10" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" />
            </svg>
            <span>Spentra</span>
          </div>
          <p>© {new Date().getFullYear()} Spentra Expense Tracker. All rights reserved.</p>
          <div className="flex items-center gap-6">
            <Link href="/login" className="hover:text-on-surface hover:underline underline-offset-4 transition-colors">
              Ledger
            </Link>
            <Link href="/signup" className="hover:text-on-surface hover:underline underline-offset-4 transition-colors">
              Access
            </Link>
            <a href="#sandbox" className="hover:text-on-surface hover:underline underline-offset-4 transition-colors">
              Sandbox
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
