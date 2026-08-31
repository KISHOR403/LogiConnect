import React from 'react';
import { Outlet } from 'react-router-dom';
import { Truck, ShieldCheck, Lock, Activity } from 'lucide-react';

export const AuthLayout: React.FC = () => {
  return (
    <div className="min-h-screen w-full flex bg-slate-900 text-white">
      {/* Left side: Enterprise Logistics Brand & System Status */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 bg-gradient-to-br from-slate-900 via-slate-850 to-brand-950 border-r border-slate-800 relative overflow-hidden">
        {/* Background ambient lighting */}
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-96 h-96 bg-brand-600/10 rounded-full blur-3xl pointer-events-none" />

        {/* Top: Logo & Title */}
        <div className="flex items-center gap-3 relative z-10">
          <div className="w-11 h-11 rounded-xl bg-brand-600 flex items-center justify-center text-white shadow-lg shadow-brand-600/30">
            <Truck size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">LogiConnect</h1>
            <p className="text-xs text-brand-400 font-medium">Enterprise Logistics Collaboration Platform</p>
          </div>
        </div>

        {/* Center: System Highlights */}
        <div className="space-y-8 max-w-md relative z-10 my-auto py-12">
          <div className="space-y-3">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-semibold text-brand-300 bg-brand-900/60 rounded-full border border-brand-700/50">
              <Activity size={12} className="text-brand-400" />
              Real-Time Fleet & Ops Sync
            </span>
            <h2 className="text-3xl font-bold text-white tracking-tight leading-tight">
              Unified communication for dispatch, hubs, and leadership.
            </h2>
            <p className="text-sm text-slate-400 leading-relaxed">
              Replacing fragmented messaging with secure, auditable broadcasts, department channels, and operations direct messaging.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800/80">
            <div className="flex items-start gap-2.5">
              <div className="p-2 rounded-lg bg-slate-800/80 text-brand-400 mt-0.5">
                <ShieldCheck size={16} />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Zero-Trust Access</h3>
                <p className="text-[11px] text-slate-400 mt-0.5">Granular RBAC & department targeting</p>
              </div>
            </div>

            <div className="flex items-start gap-2.5">
              <div className="p-2 rounded-lg bg-slate-800/80 text-brand-400 mt-0.5">
                <Lock size={16} />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Compliance Audit</h3>
                <p className="text-[11px] text-slate-400 mt-0.5">Tamper-evident logs & acknowledgements</p>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom: Security & Footer Notice */}
        <div className="flex items-center justify-between text-xs text-slate-400 pt-6 border-t border-slate-800/60 relative z-10">
          <span>&copy; {new Date().getFullYear()} LogiConnect Platform</span>
          <span className="flex items-center gap-1 text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Internal Network Secure
          </span>
        </div>
      </div>

      {/* Right side: Login / Auth Form Container */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12 bg-white text-slate-900">
        <div className="w-full max-w-md">
          <Outlet />
        </div>
      </div>
    </div>
  );
};
