import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  LayoutDashboard,
  MessageSquare,
  Hash,
  Megaphone,
  Calendar,
  Users,
  FileText,
  Bell,
  Shield,
  Building2,
  Network,
  UserCog,
  FileSpreadsheet,
  ChevronLeft,
  ChevronRight,
  Truck,
  X,
} from 'lucide-react';
import { NavItem } from '@/components/navigation/NavItem';
import { NavSection } from '@/types/navigation';
import { ROUTES } from '@/lib/constants/routes';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { isAdmin } from '@/lib/auth/permissions';
import { cn } from '@/lib/utils/cn';

export interface SidebarProps {
  mobileOpen?: boolean;
  onMobileClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  mobileOpen = false,
  onMobileClose,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const { user } = useAuth();
  const userIsAdmin = isAdmin(user);

  const mainSections: NavSection[] = [
    {
      id: 'core',
      title: 'Workspace',
      items: [
        {
          id: 'dashboard',
          label: 'Dashboard',
          path: ROUTES.DASHBOARD,
          icon: LayoutDashboard,
          exact: true,
        },
        {
          id: 'messages',
          label: 'Messages',
          path: ROUTES.MESSAGES,
          icon: MessageSquare,
        },
        {
          id: 'channels',
          label: 'Channels',
          path: ROUTES.CHANNELS,
          icon: Hash,
        },
        {
          id: 'announcements',
          label: 'Announcements',
          path: ROUTES.ANNOUNCEMENTS,
          icon: Megaphone,
        },
        {
          id: 'meetings',
          label: 'Meetings',
          path: ROUTES.MEETINGS,
          icon: Calendar,
        },
      ],
    },
    {
      id: 'organization',
      title: 'Organization',
      items: [
        {
          id: 'employees',
          label: 'Directory',
          path: ROUTES.EMPLOYEES,
          icon: Users,
        },
        {
          id: 'documents',
          label: 'Documents',
          path: ROUTES.DOCUMENTS,
          icon: FileText,
        },
        {
          id: 'notifications',
          label: 'Notifications',
          path: ROUTES.NOTIFICATIONS,
          icon: Bell,
        },
      ],
    },
  ];

  const adminSection: NavSection = {
    id: 'admin',
    title: 'Administration',
    roles: ['SUPER_ADMIN', 'HR_ADMIN'],
    items: [
      {
        id: 'admin-users',
        label: 'Users & Access',
        path: ROUTES.ADMIN_USERS,
        icon: UserCog,
      },
      {
        id: 'admin-employees',
        label: 'Employee Master',
        path: ROUTES.ADMIN_EMPLOYEES,
        icon: Users,
      },
      {
        id: 'admin-departments',
        label: 'Departments',
        path: ROUTES.ADMIN_DEPARTMENTS,
        icon: Building2,
      },
      {
        id: 'admin-teams',
        label: 'Teams & Hubs',
        path: ROUTES.ADMIN_TEAMS,
        icon: Network,
      },
      {
        id: 'admin-roles',
        label: 'Role Permissions',
        path: ROUTES.ADMIN_ROLES,
        icon: Shield,
      },
      {
        id: 'admin-audit',
        label: 'Audit Logs',
        path: ROUTES.ADMIN_AUDIT_LOGS,
        icon: FileSpreadsheet,
      },
    ],
  };

  const content = (
    <div className="flex flex-col h-full bg-slate-900 text-slate-200">
      {/* Brand Header */}
      <div className="flex items-center justify-between h-16 px-4 border-b border-slate-800 shrink-0">
        <Link
          to={ROUTES.DASHBOARD}
          className="flex items-center gap-3 focus-ring rounded-lg overflow-hidden"
          onClick={onMobileClose}
        >
          <div className="w-9 h-9 rounded-xl bg-brand-600 flex items-center justify-center text-white shrink-0 shadow-md">
            <Truck size={20} />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col">
              <span className="font-bold text-base text-white tracking-tight leading-tight">
                LogiConnect
              </span>
              <span className="text-[10px] uppercase font-semibold tracking-wider text-brand-400">
                Operations
              </span>
            </div>
          )}
        </Link>

        {/* Mobile close button */}
        <button
          onClick={onMobileClose}
          className="p-1.5 text-slate-400 hover:text-white rounded-lg lg:hidden"
          aria-label="Close sidebar"
        >
          <X size={18} />
        </button>

        {/* Desktop Collapse Toggle */}
        <button
          onClick={() => setIsCollapsed((prev) => !prev)}
          className="hidden lg:flex p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors focus-ring"
          aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {isCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </div>

      {/* Navigation Links */}
      <div className="flex-1 overflow-y-auto py-4 px-3 space-y-6">
        {mainSections.map((section) => (
          <div key={section.id} className="space-y-1">
            {!isCollapsed && section.title && (
              <h4 className="px-3 text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1.5">
                {section.title}
              </h4>
            )}
            {section.items.map((item) => (
              <NavItem
                key={item.id}
                item={item}
                isCollapsed={isCollapsed}
                onNavigate={onMobileClose}
              />
            ))}
          </div>
        ))}

        {/* Admin Section (Conditional on Admin Roles) */}
        {userIsAdmin && (
          <div className="pt-2 border-t border-slate-800 space-y-1">
            {!isCollapsed && (
              <h4 className="px-3 text-[10px] font-bold uppercase tracking-wider text-amber-400 mb-1.5 flex items-center gap-1.5">
                <Shield size={11} />
                Administration
              </h4>
            )}
            {adminSection.items.map((item) => (
              <NavItem
                key={item.id}
                item={item}
                isCollapsed={isCollapsed}
                onNavigate={onMobileClose}
              />
            ))}
          </div>
        )}
      </div>

      {/* Footer / Environment Badge */}
      <div className="p-3 border-t border-slate-800 text-[11px] text-slate-400 shrink-0">
        {!isCollapsed ? (
          <div className="flex items-center justify-between">
            <span className="font-medium">Logistics Core</span>
            <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[10px]">
              v1.0
            </span>
          </div>
        ) : (
          <div className="text-center font-mono text-[9px] text-slate-400">v1.0</div>
        )}
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Persistent Sidebar */}
      <aside
        className={cn(
          'hidden lg:flex flex-col border-r border-slate-800 shrink-0 transition-all duration-200 z-40',
          isCollapsed ? 'w-18' : 'w-64'
        )}
      >
        {content}
      </aside>

      {/* Mobile Drawer Backdrop and Sidebar */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-50 lg:hidden bg-slate-950/60 backdrop-blur-xs animate-in fade-in duration-200"
          onClick={onMobileClose}
        >
          <div
            className="fixed inset-y-0 left-0 w-72 max-w-[85vw] shadow-2xl animate-in slide-in-from-left duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            {content}
          </div>
        </div>
      )}
    </>
  );
};
