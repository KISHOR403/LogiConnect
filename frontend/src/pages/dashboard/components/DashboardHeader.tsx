import React from 'react';
import { Shield, Building, MapPin } from 'lucide-react';
import { CurrentUser } from '@/types/auth';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/feedback/Skeleton';
import { ROLE_BADGE_VARIANTS, ROLE_LABELS } from '@/lib/constants/roles';

export interface DashboardHeaderProps {
  user: CurrentUser | null;
  isLoading?: boolean;
}

export const DashboardHeader: React.FC<DashboardHeaderProps> = ({ user, isLoading = false }) => {
  if (isLoading || !user) {
    return (
      <div className="bg-slate-900 rounded-2xl p-6 sm:p-8 text-white border border-slate-800 animate-pulse">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <Skeleton variant="rectangular" width={120} height={18} className="bg-slate-800" />
            <Skeleton variant="rectangular" width={80} height={18} className="bg-slate-800" />
          </div>
          <Skeleton variant="text" width="45%" height={32} className="bg-slate-800" />
          <Skeleton variant="text" width="30%" height={16} className="bg-slate-800" />
        </div>
      </div>
    );
  }

  const primaryRole = user.roles && user.roles.length > 0 ? user.roles[0] : 'EMPLOYEE';
  const roleLabel = ROLE_LABELS[primaryRole] || primaryRole;
  const roleVariant = ROLE_BADGE_VARIANTS[primaryRole] || 'neutral';
  const firstName = user.firstName || user.name?.split(' ')[0] || 'Colleague';

  return (
    <div className="bg-gradient-to-r from-slate-900 via-slate-850 to-brand-950 rounded-2xl p-6 sm:p-8 text-white shadow-sm border border-slate-800">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1.5">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold tracking-wider text-brand-400 uppercase">
              Operations Workspace
            </span>
            <span className="text-slate-500">•</span>
            <Badge variant={roleVariant} size="sm" className="bg-slate-800/90 border-slate-700 text-slate-200">
              <Shield size={10} className="mr-0.5" />
              {roleLabel}
            </Badge>
          </div>

          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Welcome back, {firstName}
          </h1>

          <div className="text-xs sm:text-sm text-slate-300 flex flex-wrap items-center gap-2">
            {user.employeeCode && (
              <span className="font-mono bg-slate-800/80 px-2 py-0.5 rounded text-slate-300 border border-slate-700/60">
                {user.employeeCode}
              </span>
            )}
            {user.department && (
              <>
                <span className="text-slate-500">•</span>
                <span className="flex items-center gap-1">
                  <Building size={13} className="text-brand-400" />
                  {user.department.name}
                </span>
              </>
            )}
            {user.location && (
              <>
                <span className="text-slate-500">•</span>
                <span className="flex items-center gap-1 text-slate-400">
                  <MapPin size={13} className="text-slate-400" />
                  {user.location}
                </span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
