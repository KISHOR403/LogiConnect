import React, { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, Settings, KeyRound, LogOut, ChevronDown, Shield } from 'lucide-react';
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

  const handleLogout = async () => {
    setIsOpen(false);
    await logout();
    navigate(ROUTES.LOGIN, { replace: true });
  };

  return (
    <div className="relative" ref={menuRef}>
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center gap-2.5 p-1.5 rounded-lg hover:bg-slate-100 transition-colors focus-ring"
        aria-expanded={isOpen}
        aria-haspopup="true"
        aria-label="User profile menu"
      >
        <Avatar name={user.name || user.email} size="sm" status="online" />
        <div className="hidden md:flex flex-col text-left">
          <span className="text-xs font-semibold text-slate-800 leading-tight truncate max-w-[130px]">
            {user.name}
          </span>
          <span className="text-[10px] text-slate-500 leading-tight truncate max-w-[130px]">
            {user.employeeCode}
          </span>
        </div>
        <ChevronDown size={14} className={cn('text-slate-400 transition-transform duration-150', isOpen ? 'rotate-180' : '')} />
      </button>

      {isOpen && (
        <div
          className="absolute right-0 mt-2 w-72 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-50 animate-in fade-in slide-in-from-top-1 duration-150"
          role="menu"
          aria-orientation="vertical"
        >
          {/* User Info Header */}
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50/50">
            <p className="text-sm font-semibold text-slate-900 truncate">{user.name}</p>
            <p className="text-xs text-slate-500 truncate">{user.email}</p>
            <div className="flex flex-wrap items-center gap-1.5 mt-2">
              <Badge variant={roleVariant} size="sm">
                <Shield size={10} className="mr-0.5" />
                {roleLabel}
              </Badge>
              {user.department && (
                <span className="text-[11px] font-medium text-slate-600 bg-slate-200/70 px-2 py-0.5 rounded-full">
                  {user.department.name}
                </span>
              )}
            </div>
          </div>

          {/* Menu Items */}
          <div className="py-1">
            <Link
              to={ROUTES.PROFILE}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <User size={15} className="text-slate-400" />
              My Profile
            </Link>

            <Link
              to={ROUTES.SETTINGS}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <Settings size={15} className="text-slate-400" />
              Settings
            </Link>

            <Link
              to={`${ROUTES.SETTINGS}?tab=password`}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition-colors"
              role="menuitem"
            >
              <KeyRound size={15} className="text-slate-400" />
              Change Password
            </Link>
          </div>

          {/* Sign Out */}
          <div className="pt-1 border-t border-slate-100">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors text-left"
              role="menuitem"
            >
              <LogOut size={15} className="text-red-500" />
              Sign Out
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
