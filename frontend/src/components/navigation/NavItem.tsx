import React from 'react';
import { NavLink } from 'react-router-dom';
import { NavItem as NavItemType } from '@/types/navigation';
import { cn } from '@/lib/utils/cn';

export interface NavItemProps {
  item: NavItemType;
  isCollapsed?: boolean;
  onNavigate?: () => void;
}

export const NavItem: React.FC<NavItemProps> = ({
  item,
  isCollapsed = false,
  onNavigate,
}) => {
  const Icon = item.icon;

  return (
    <NavLink
      to={item.path}
      end={item.exact}
      onClick={onNavigate}
      title={isCollapsed ? item.label : undefined}
      className={({ isActive }) =>
        cn(
          'group relative flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 select-none focus-ring',
          isActive
            ? 'bg-brand-50 text-brand-700 font-semibold shadow-xs'
            : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80',
          isCollapsed ? 'justify-center px-2' : ''
        )
      }
    >
      {({ isActive }) => (
        <>
          {/* Left active border indicator */}
          {isActive && (
            <span
              className="absolute left-0 top-1.5 bottom-1.5 w-1 bg-brand-600 rounded-r-full"
              aria-hidden="true"
            />
          )}

          <Icon
            size={18}
            className={cn(
              'shrink-0 transition-colors',
              isActive ? 'text-brand-600' : 'text-slate-400 group-hover:text-slate-600'
            )}
            aria-hidden="true"
          />

          {!isCollapsed && (
            <span className="flex-1 truncate text-left">{item.label}</span>
          )}

          {!isCollapsed && item.badge !== undefined && (
            <span
              className={cn(
                'ml-auto px-2 py-0.5 text-xs font-semibold rounded-full shrink-0',
                isActive
                  ? 'bg-brand-200 text-brand-800'
                  : 'bg-slate-200 text-slate-700 group-hover:bg-slate-300'
              )}
            >
              {item.badge}
            </span>
          )}

          {/* Collapsed mode badge dot */}
          {isCollapsed && item.badge !== undefined && (
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-brand-600 ring-2 ring-white" />
          )}
        </>
      )}
    </NavLink>
  );
};
