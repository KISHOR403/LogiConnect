import React from 'react';
import { Outlet } from 'react-router-dom';
import { Truck, ShieldCheck, FileCheck, MessageSquare, Users, Megaphone, Calendar } from 'lucide-react';

export const AuthLayout: React.FC = () => {
  return (
    <div className="min-h-screen w-full flex bg-slate-900 text-white">
      {/* Left side: Enterprise Platform Overview & Capabilities */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 bg-slate-900 border-r border-slate-800 relative">
        {/* Top: Logo & Title */}
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-brand-600 flex items-center justify-center text-white shadow-sm">
            <Truck size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">LogiConnect</h1>
            <p className="text-xs text-slate-400 font-medium">Enterprise Internal Communication Platform</p>
          </div>
        </div>

        {/* Center: Messaging & Product Capabilities */}
        <div className="space-y-8 max-w-lg my-auto py-8">
          <div className="space-y-3">
            <h2 className="text-3xl font-bold text-white tracking-tight leading-tight">
              Secure communication for your workplace
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              Connect with your teams, share operational updates, manage announcements, and coordinate meetings — all in one secure platform.
            </p>
          </div>

          {/* 4 Concise Product Capabilities */}
          <div className="space-y-3 pt-2">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Core Capabilities
            </p>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex items-center gap-2.5 p-3 rounded-lg bg-slate-800/80 border border-slate-700/60">
                <MessageSquare size={16} className="text-brand-400 shrink-0" />
                <span className="text-xs font-medium text-slate-200">Secure employee messaging</span>
              </div>
              <div className="flex items-center gap-2.5 p-3 rounded-lg bg-slate-800/80 border border-slate-700/60">
                <Users size={16} className="text-brand-400 shrink-0" />
                <span className="text-xs font-medium text-slate-200">Department & team channels</span>
              </div>
              <div className="flex items-center gap-2.5 p-3 rounded-lg bg-slate-800/80 border border-slate-700/60">
                <Megaphone size={16} className="text-brand-400 shrink-0" />
                <span className="text-xs font-medium text-slate-200">Company announcements</span>
              </div>
              <div className="flex items-center gap-2.5 p-3 rounded-lg bg-slate-800/80 border border-slate-700/60">
                <Calendar size={16} className="text-brand-400 shrink-0" />
                <span className="text-xs font-medium text-slate-200">Meetings & collaboration</span>
              </div>
            </div>
          </div>

          {/* Governance & Security Highlights */}
          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800">
            <div className="flex items-start gap-2.5">
              <div className="p-2 rounded-lg bg-slate-800 text-brand-400 mt-0.5">
                <ShieldCheck size={16} />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Role-Based Access</h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Permissions based on your role and responsibilities.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-2.5">
              <div className="p-2 rounded-lg bg-slate-800 text-brand-400 mt-0.5">
                <FileCheck size={16} />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Audit & Accountability</h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Important activities are recorded for organizational accountability.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom: Security & Footer Notice */}
        <div className="flex items-center justify-between text-xs text-slate-400 pt-6 border-t border-slate-800">
          <span>&copy; {new Date().getFullYear()} LogiConnect Platform</span>
          <span className="flex items-center gap-1.5 text-emerald-400 font-medium">
            <span className="w-2 h-2 rounded-full bg-emerald-400" />
            Secure Enterprise Platform
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
