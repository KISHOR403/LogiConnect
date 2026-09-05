import React, { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  User,
  Settings,
  Shield,
  ShieldCheck,
  Laptop,
  HelpCircle,
  LogOut,
  ChevronDown,
} from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Avatar } from '@/components/common/Avatar';
import { Badge } from '@/components/ui/Badge';
import { ROLE_BADGE_VARIANTS, ROLE_LABELS } from '@/lib/constants/roles';
import { ROUTES } from '@/lib/constants/routes';
import { useOnClickOutside } from '@/hooks/useOnClickOutside';
import { cn } from '@/lib/utils/cn';

export const UserMenu: React.FC = () => {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useOnClickOutside(menuRef, () => setIsOpen(false), isOpen);

  if (!user) return null;

  const primaryRole = user.roles && user.roles.length > 0 ? user.roles[0] : 'EMPLOYEE';
  const roleLabel = ROLE_LABELS[primaryRole] || primaryRole;
  const roleVariant = ROLE_BADGE_VARIANTS[primaryRole] || 'neutral';
  const displayName = user.name || `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;

  const handleLogout = async () => {
    setIsOpen(false);
    await logout();
    navigate(ROUTES.LOGIN, { replace: true });
  };

  return (
    <div className="relative" ref={menuRef}>
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center gap-2.5 p-1.5 rounded-lg hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        aria-expanded={isOpen}
        aria-haspopup="true"
        aria-label="User profile menu"
      >
        <Avatar name={displayName} size="sm" status="online" />
        <div className="hidden md:flex flex-col text-left">
          <span className="text-xs font-semibold text-slate-800 leading-tight truncate max-w-[130px]">
            {displayName}
          </span>
          <span className="text-[10px] text-slate-500 leading-tight truncate max-w-[130px]">
            {user.employeeCode || 'Employee'}
          </span>
        </div>
        <ChevronDown
          size={14}
          className={cn('text-slate-400 transition-transform duration-150', isOpen ? 'rotate-180' : '')}
        />
      </button>

      {isOpen && (
        <div
          className="absolute right-0 mt-2 w-72 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-50 animate-in fade-in slide-in-from-top-1 duration-150"
          role="menu"
          aria-orientation="vertical"
        >
          {/* User Info Header */}
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50/50">
            <div className="flex items-center gap-3">
              <Avatar name={displayName} size="md" status="online" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-900 truncate">{displayName}</p>
                <p className="text-xs text-slate-500 truncate">{user.email}</p>
                <p className="text-[11px] font-mono text-slate-400 mt-0.5">{user.employeeCode}</p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-1.5 mt-2.5 pt-2 border-t border-slate-200/60">
              <Badge variant={roleVariant} size="sm">
                <Shield size={10} className="mr-0.5" />
                {roleLabel}
              </Badge>
              {user.department && (
                <span className="text-[11px] font-medium text-slate-600 bg-slate-200/70 px-2 py-0.5 rounded-full truncate max-w-[140px]">
                  {user.department.name}
                </span>
              )}
            </div>
          </div>

          {/* Menu Items */}
          <div className="py-1" role="none">
            <Link
              to={ROUTES.PROFILE}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <User size={15} className="text-slate-400" />
              <span>My Profile</span>
            </Link>

            <Link
              to={ROUTES.SETTINGS}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <Settings size={15} className="text-slate-400" />
              <span>Account Settings</span>
            </Link>

            <Link
              to={`${ROUTES.SETTINGS}?tab=password`}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <ShieldCheck size={15} className="text-slate-400" />
              <span>Security</span>
            </Link>

            <Link
              to={`${ROUTES.SETTINGS}?tab=sessions`}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <Laptop size={15} className="text-slate-400" />
              <span>Active Sessions</span>
            </Link>

            <a
              href="mailto:support@logiconnect.internal"
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <HelpCircle size={15} className="text-slate-400" />
              <span>Help & Support</span>
            </a>
          </div>

          {/* Sign Out */}
          <div className="pt-1 border-t border-slate-100" role="none">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors text-left"
              role="menuitem"
            >
              <LogOut size={15} className="text-red-500" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
