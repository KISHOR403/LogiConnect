import React from 'react';
import { Shield, Building, Network, Mail, MapPin, BadgeCheck } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/common/Avatar';
import { ROLE_BADGE_VARIANTS, ROLE_LABELS } from '@/lib/constants/roles';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  if (!user) return null;

  const primaryRole = user.roles?.[0] || 'EMPLOYEE';
  const roleLabel = ROLE_LABELS[primaryRole] || primaryRole;
  const roleVariant = ROLE_BADGE_VARIANTS[primaryRole] || 'neutral';

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">My Employee Profile</h1>
        <p className="text-sm text-slate-500">View your operational credentials, department assignment, and assigned permissions.</p>
      </div>

      <Card>
        <CardContent className="p-6 sm:p-8">
          <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6">
            <Avatar name={user.name || user.email} size="xl" status="online" />
            <div className="space-y-2 text-center sm:text-left flex-1">
              <div className="flex flex-col sm:flex-row sm:items-center gap-2.5">
                <h2 className="text-xl font-bold text-slate-900">{user.name}</h2>
                <Badge variant={roleVariant} size="sm">
                  <Shield size={11} className="mr-0.5" />
                  {roleLabel}
                </Badge>
              </div>

              <p className="text-xs text-slate-500 font-mono">
                Employee Code: <span className="font-semibold text-slate-700">{user.employeeCode}</span>
              </p>

              {user.designation && (
                <p className="text-sm font-medium text-slate-700">{user.designation}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-8 pt-8 border-t border-slate-100">
            <div className="flex items-center gap-3 p-3.5 rounded-lg bg-slate-50 border border-slate-100">
              <Mail size={18} className="text-slate-400 shrink-0" />
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase text-slate-400">Email Address</p>
                <p className="text-xs font-medium text-slate-800 truncate">{user.email}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3.5 rounded-lg bg-slate-50 border border-slate-100">
              <Building size={18} className="text-slate-400 shrink-0" />
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase text-slate-400">Department</p>
                <p className="text-xs font-medium text-slate-800 truncate">
                  {user.department ? `${user.department.name} (${user.department.code})` : 'Unassigned'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3.5 rounded-lg bg-slate-50 border border-slate-100">
              <Network size={18} className="text-slate-400 shrink-0" />
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase text-slate-400">Team / Hub</p>
                <p className="text-xs font-medium text-slate-800 truncate">
                  {user.team ? `${user.team.name} (${user.team.code})` : 'Unassigned'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3.5 rounded-lg bg-slate-50 border border-slate-100">
              <MapPin size={18} className="text-slate-400 shrink-0" />
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase text-slate-400">Location</p>
                <p className="text-xs font-medium text-slate-800 truncate">
                  {user.location || 'Headquarters / Main Hub'}
                </p>
              </div>
            </div>
          </div>

          {/* Assigned Permissions */}
          {user.permissions && user.permissions.length > 0 && (
            <div className="mt-8 pt-6 border-t border-slate-100">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                <BadgeCheck size={14} className="text-brand-500" />
                Active Security Permissions
              </h3>
              <div className="flex flex-wrap gap-1.5">
                {user.permissions.map((perm) => (
                  <span
                    key={perm}
                    className="px-2.5 py-1 text-[11px] font-mono font-medium rounded bg-slate-100 text-slate-700 border border-slate-200"
                  >
                    {perm}
                  </span>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
