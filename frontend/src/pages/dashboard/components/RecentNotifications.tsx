import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Bell, ArrowRight, Clock } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Skeleton } from '@/components/feedback/Skeleton';
import { ErrorState } from '@/components/feedback/ErrorState';
import { EmptyState } from '@/components/feedback/EmptyState';
import { notificationApi } from '@/features/notifications/api/notificationApi';
import { NotificationItem } from '@/types/notification';
import { formatRelativeTime } from '@/lib/utils/formatDate';
import { ROUTES } from '@/lib/constants/routes';

export const RecentNotifications: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchNotifications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await notificationApi.getNotifications(0, 5);
      setNotifications(response.content || []);
    } catch {
      setError('Unable to load notifications.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-100">
        <div>
          <CardTitle className="text-base flex items-center gap-2">
            <Bell size={17} className="text-rose-500" />
            <span>Recent Notifications</span>
          </CardTitle>
        </div>
        <Link
          to={ROUTES.NOTIFICATIONS}
          className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-500 rounded"
        >
          <span>View all</span>
          <ArrowRight size={13} />
        </Link>
      </CardHeader>

      <CardContent className="pt-4">
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-1.5 py-2">
                <Skeleton variant="text" width="70%" height={14} />
                <Skeleton variant="text" width="40%" height={10} />
              </div>
            ))}
          </div>
        ) : error ? (
          <ErrorState
            title="Unable to load notifications"
            message={error}
            onRetry={fetchNotifications}
          />
        ) : notifications.length === 0 ? (
          <EmptyState
            compact
            icon={Bell}
            title="You're all caught up"
            description="No new notifications or alerts."
          />
        ) : (
          <div className="divide-y divide-slate-100">
            {notifications.map((item) => (
              <div key={item.id} className="py-2.5 first:pt-0 last:pb-0">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-xs font-semibold text-slate-800 line-clamp-1">
                    {item.title}
                  </p>
                  {!item.isRead && (
                    <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shrink-0 mt-1" />
                  )}
                </div>
                {item.content && (
                  <p className="text-xs text-slate-500 line-clamp-2 mt-0.5">
                    {item.content}
                  </p>
                )}
                <span className="text-[10px] text-slate-400 font-medium mt-1 flex items-center gap-1">
                  <Clock size={10} />
                  {formatRelativeTime(item.createdAt)}
                </span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
