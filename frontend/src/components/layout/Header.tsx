import React from 'react';
import { Menu, Truck } from 'lucide-react';
import { SearchBar } from '@/components/common/SearchBar';
import { NotificationDropdown } from './NotificationDropdown';
import { UserMenu } from './UserMenu';

export interface HeaderProps {
  onMenuToggle: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onMenuToggle }) => {
  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-200 bg-white/95 px-4 sm:px-6 backdrop-blur-xs">
      {/* Left: Mobile Drawer Trigger & Brand on small screen */}
      <div className="flex items-center gap-3 lg:gap-0">
        <button
          onClick={onMenuToggle}
          className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg lg:hidden focus-ring"
          aria-label="Toggle navigation drawer"
        >
          <Menu size={20} />
        </button>

        <div className="flex items-center gap-2 lg:hidden">
          <div className="w-8 h-8 rounded-lg bg-brand-600 flex items-center justify-center text-white shadow-xs">
            <Truck size={18} />
          </div>
          <span className="font-bold text-base text-slate-900 tracking-tight">LogiConnect</span>
        </div>
      </div>

      {/* Middle: Global Search */}
      <div className="hidden sm:flex flex-1 max-w-xl mx-4">
        <SearchBar />
      </div>

      {/* Right: Notifications & User Profile Menu */}
      <div className="flex items-center gap-2 sm:gap-3">
        <NotificationDropdown />
        <div className="h-6 w-px bg-slate-200" aria-hidden="true" />
        <UserMenu />
      </div>
    </header>
  );
};
