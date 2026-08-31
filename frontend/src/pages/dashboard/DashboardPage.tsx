import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MessageSquare,
  Megaphone,
  Calendar,
  Users,
  Bell,
  ArrowRight,
  Shield,
  Building,
  Plus,
} from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/feedback/EmptyState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { notificationApi } from '@/features/notifications/api/notificationApi';
import { NotificationItem } from '@/types/notification';
import { formatRelativeTime } from '@/lib/utils/formatDate';
import { ROLE_BADGE_VARIANTS, ROLE_LABELS } from '@/lib/constants/roles';
import { ROUTES } from '@/lib/constants/routes';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loadingNotifications, setLoadingNotifications] = useState<boolean>(true);

  useEffect(() => {
    const fetchRecentNotifications = async () => {
      try {
        const response = await notificationApi.getNotifications(0, 5);
        setNotifications(response.content || []);
      } catch {
        setNotifications([]);
      } finally {
        setLoadingNotifications(false);
      }
    };

    fetchRecentNotifications();
  }, []);

  const primaryRole = user?.roles?.[0] || 'EMPLOYEE';
  const roleLabel = ROLE_LABELS[primaryRole] || primaryRole;
  const roleVariant = ROLE_BADGE_VARIANTS[primaryRole] || 'neutral';

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-850 to-brand-950 rounded-2xl p-6 sm:p-8 text-white shadow-sm border border-slate-800">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold tracking-wider text-brand-400 uppercase">
                Operations Workspace
              </span>
              <span className="text-slate-500">•</span>
              <Badge variant={roleVariant} size="sm" className="bg-slate-800/80 border-slate-700 text-slate-200">
                <Shield size={10} className="mr-0.5" />
                {roleLabel}
              </Badge>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Welcome back, {user?.firstName || user?.name || 'Employee'}
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 flex items-center gap-2">
              <span>{user?.employeeCode}</span>
              {user?.department && (
                <>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <Building size={12} className="text-brand-400" />
                    {user.department.name}
                  </span>
                </>
              )}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Link to={ROUTES.MESSAGES}>
              <Button variant="primary" size="sm" leftIcon={<Plus size={14} />}>
                New Message
              </Button>
            </Link>
            <Link to={ROUTES.ANNOUNCEMENTS}>
              <Button
                variant="outline"
                size="sm"
                className="border-slate-700 bg-slate-800/80 text-slate-200 hover:bg-slate-800 hover:text-white"
              >
                View Broadcasts
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Quick Action Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <Link to={ROUTES.MESSAGES} className="group focus-ring rounded-xl">
          <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
            <div className="p-2.5 rounded-lg bg-blue-50 text-blue-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <MessageSquare size={20} />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
                Direct Messages
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">Chat 1-on-1 or in groups</p>
            </div>
          </Card>
        </Link>

        <Link to={ROUTES.ANNOUNCEMENTS} className="group focus-ring rounded-xl">
          <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
            <div className="p-2.5 rounded-lg bg-amber-50 text-amber-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <Megaphone size={20} />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
                Announcements
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">Company & team updates</p>
            </div>
          </Card>
        </Link>

        <Link to={ROUTES.MEETINGS} className="group focus-ring rounded-xl">
          <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
            <div className="p-2.5 rounded-lg bg-purple-50 text-purple-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <Calendar size={20} />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
                Meetings
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">Schedule operational syncs</p>
            </div>
          </Card>
        </Link>

        <Link to={ROUTES.EMPLOYEES} className="group focus-ring rounded-xl">
          <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
            <div className="p-2.5 rounded-lg bg-emerald-50 text-emerald-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <Users size={20} />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
                Directory
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">Search 2,000+ colleagues</p>
            </div>
          </Card>
        </Link>
      </div>

      {/* Main Grid: Messages, Meetings, Announcements, Notifications */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Columns: Messaging & Announcements */}
        <div className="lg:col-span-2 space-y-6">
          {/* Latest Announcements Section */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="text-base flex items-center gap-2">
                  <Megaphone size={17} className="text-amber-500" />
                  Latest Official Announcements
                </CardTitle>
              </div>
              <Link
                to={ROUTES.ANNOUNCEMENTS}
                className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1"
              >
                View all <ArrowRight size={13} />
              </Link>
            </CardHeader>
            <CardContent>
              <EmptyState
                compact
                icon={Megaphone}
                title="No active broadcasts"
                description="Official company announcements, policy updates, and shift memos will appear here."
              />
            </CardContent>
          </Card>

          {/* Recent Messages Section */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="text-base flex items-center gap-2">
                  <MessageSquare size={17} className="text-blue-500" />
                  Recent Conversations
                </CardTitle>
              </div>
              <Link
                to={ROUTES.MESSAGES}
                className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1"
              >
                Open chat <ArrowRight size={13} />
              </Link>
            </CardHeader>
            <CardContent>
              <EmptyState
                compact
                icon={MessageSquare}
                title="No conversations yet"
                description="Start a direct message with a colleague or join a departmental channel."
                actionLabel="Start Direct Message"
                onAction={() => {
                  window.location.href = ROUTES.MESSAGES;
                }}
              />
            </CardContent>
          </Card>
        </div>

        {/* Right 1 Column: Notifications & Upcoming Meetings */}
        <div className="space-y-6">
          {/* Notifications Feed */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="text-base flex items-center gap-2">
                  <Bell size={17} className="text-rose-500" />
                  Recent Notifications
                </CardTitle>
              </div>
              <Link
                to={ROUTES.NOTIFICATIONS}
                className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1"
              >
                View all <ArrowRight size={13} />
              </Link>
            </CardHeader>
            <CardContent>
              {loadingNotifications ? (
                <div className="py-8 flex justify-center items-center">
                  <LoadingSpinner size="md" />
                </div>
              ) : notifications.length === 0 ? (
                <EmptyState
                  compact
                  icon={Bell}
                  title="No notifications"
                  description="You are caught up on all alerts and messages."
                />
              ) : (
                <div className="divide-y divide-slate-100">
                  {notifications.map((item) => (
                    <div key={item.id} className="py-2.5 first:pt-0 last:pb-0">
                      <p className="text-xs font-semibold text-slate-800 line-clamp-1">
                        {item.title}
                      </p>
                      {item.content && (
                        <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">
                          {item.content}
                        </p>
                      )}
                      <span className="text-[10px] text-slate-400 font-medium mt-1 block">
                        {formatRelativeTime(item.createdAt)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Upcoming Meetings */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="text-base flex items-center gap-2">
                  <Calendar size={17} className="text-purple-500" />
                  Upcoming Meetings
                </CardTitle>
              </div>
              <Link
                to={ROUTES.MEETINGS}
                className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1"
              >
                Calendar <ArrowRight size={13} />
              </Link>
            </CardHeader>
            <CardContent>
              <EmptyState
                compact
                icon={Calendar}
                title="No meetings scheduled"
                description="Your upcoming operational syncs and shift briefings will show here."
              />
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
