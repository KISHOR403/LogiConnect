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
  const hasBadge = item.badge !== undefined && (typeof item.badge === 'number' ? item.badge > 0 : Boolean(item.badge));

  return (
    <NavLink
      to={item.path}
      end={item.exact}
      onClick={onNavigate}
      title={isCollapsed ? item.label : undefined}
      className={({ isActive }) =>
        cn(
          'group relative flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 select-none',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 focus-visible:ring-offset-1 focus-visible:ring-offset-slate-900',
          isActive
            ? 'bg-slate-800 text-white font-semibold shadow-xs'
            : 'text-slate-300 hover:text-white hover:bg-slate-800/60',
          isCollapsed ? 'justify-center px-2' : ''
        )
      }
    >
      {({ isActive }) => (
        <>
          {/* Active border indicator */}
          {isActive && (
            <span
              className="absolute left-0 top-1.5 bottom-1.5 w-1 bg-brand-500 rounded-r-full"
              aria-hidden="true"
            />
          )}

          <Icon
            size={18}
            className={cn(
              'shrink-0 transition-colors',
              isActive ? 'text-brand-400' : 'text-slate-400 group-hover:text-slate-200'
            )}
            aria-hidden="true"
          />

          {!isCollapsed && (
            <span className="flex-1 truncate text-left">{item.label}</span>
          )}

          {!isCollapsed && hasBadge && (
            <span
              className={cn(
                'ml-auto px-2 py-0.5 text-xs font-semibold rounded-full shrink-0',
                isActive
                  ? 'bg-brand-500/20 text-brand-300 border border-brand-500/30'
                  : 'bg-slate-800 text-slate-300 border border-slate-700 group-hover:border-slate-600'
              )}
            >
              {item.badge}
            </span>
          )}

          {/* Collapsed mode badge dot */}
          {isCollapsed && hasBadge && (
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-brand-500 ring-2 ring-slate-900" />
          )}
        </>
      )}
    </NavLink>
  );
};
