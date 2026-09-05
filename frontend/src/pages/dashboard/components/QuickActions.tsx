import React from 'react';
import { Link } from 'react-router-dom';
import {
  MessageSquarePlus,
  CalendarPlus,
  Megaphone,
  UserPlus,
  Users,
  ShieldAlert,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { CurrentUser } from '@/types/auth';
import { ROUTES } from '@/lib/constants/routes';
import { hasAnyRole } from '@/lib/auth/permissions';

export interface QuickActionsProps {
  user: CurrentUser | null;
}

export const QuickActions: React.FC<QuickActionsProps> = ({ user }) => {
  if (!user) return null;

  const isSuperAdmin = hasAnyRole(user, ['SUPER_ADMIN']);
  const isHrAdmin = hasAnyRole(user, ['HR_ADMIN']);
  const isManager = hasAnyRole(user, ['MANAGER']);
  const isTeamLead = hasAnyRole(user, ['TEAM_LEADER']);

  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-1 max-w-full sm:flex-wrap">
      {/* Standard Actions for All Staff */}
      <Link to={ROUTES.MESSAGES}>
        <Button variant="primary" size="sm" leftIcon={<MessageSquarePlus size={15} />}>
          New Message
        </Button>
      </Link>

      <Link to={ROUTES.MEETINGS}>
        <Button
          variant="outline"
          size="sm"
          leftIcon={<CalendarPlus size={15} />}
          className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
        >
          Schedule Sync
        </Button>
      </Link>

      {/* Team Leader & Manager Actions */}
      {(isTeamLead || isManager) && (
        <Link to={ROUTES.ANNOUNCEMENTS}>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<Megaphone size={15} />}
            className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Post Announcement
          </Button>
        </Link>
      )}

      {/* HR Admin Actions */}
      {isHrAdmin && (
        <Link to={ROUTES.ADMIN_EMPLOYEES}>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<UserPlus size={15} />}
            className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Onboard Employee
          </Button>
        </Link>
      )}

      {/* Super Admin Actions */}
      {isSuperAdmin && (
        <Link to={ROUTES.ADMIN_USERS}>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<ShieldAlert size={15} />}
            className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Manage Access
          </Button>
        </Link>
      )}

      {/* Common Directory action */}
      <Link to={ROUTES.EMPLOYEES} className="hidden sm:inline-flex">
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<Users size={15} />}
          className="text-slate-600 hover:text-slate-900"
        >
          Directory
        </Button>
      </Link>
    </div>
  );
};
