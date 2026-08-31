import React, { useEffect, useState } from 'react';
import { Bell, CheckCheck, Megaphone, MessageSquare, Calendar, Shield, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/feedback/EmptyState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { notificationApi } from '@/features/notifications/api/notificationApi';
import { NotificationItem, NotificationType } from '@/types/notification';
import { formatRelativeTime, formatDateTime } from '@/lib/utils/formatDate';
import { cn } from '@/lib/utils/cn';

function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case 'ANNOUNCEMENT':
    case 'URGENT_ANNOUNCEMENT':
    case 'ACKNOWLEDGEMENT_REQUIRED':
      return <Megaphone size={16} className="text-amber-600" />;
    case 'MESSAGE':
    case 'GROUP_MESSAGE':
    case 'CHANNEL_MESSAGE':
      return <MessageSquare size={16} className="text-blue-600" />;
    case 'MEETING_INVITATION':
    case 'MEETING_UPDATED':
    case 'MEETING_CANCELLED':
      return <Calendar size={16} className="text-purple-600" />;
    case 'SECURITY':
      return <Shield size={16} className="text-rose-600" />;
    default:
      return <AlertCircle size={16} className="text-slate-600" />;
  }
}

export const NotificationsPage: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadNotifications = async () => {
    setIsLoading(true);
    try {
      const res = await notificationApi.getNotifications(0, 50);
      setNotifications(res.content || []);
    } catch {
      setNotifications([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
      );
    } catch {
      // Ignore
    }
  };

  const handleMarkOne = async (id: string) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
      );
    } catch {
      // Ignore
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Notification Center</h1>
          <p className="text-sm text-slate-500">Historical feed of your operational messages, broadcast alerts, and system notifications.</p>
        </div>
        {notifications.some((n) => !n.isRead) && (
          <Button variant="outline" size="sm" onClick={handleMarkAllRead} leftIcon={<CheckCheck size={14} />}>
            Mark All as Read
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-base flex items-center gap-2">
            <Bell size={18} className="text-rose-500" />
            All Notifications
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="py-16 flex justify-center items-center">
              <LoadingSpinner size="lg" />
            </div>
          ) : notifications.length === 0 ? (
            <div className="p-12">
              <EmptyState
                icon={Bell}
                title="No notifications"
                description="You are all caught up! When you receive direct messages or official notices, they will appear here."
              />
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {notifications.map((item) => (
                <div
                  key={item.id}
                  className={cn(
                    'p-4 sm:p-5 flex items-start justify-between gap-4 transition-colors hover:bg-slate-50',
                    !item.isRead ? 'bg-brand-50/20' : ''
                  )}
                >
                  <div className="flex items-start gap-3.5 min-w-0 flex-1">
                    <div className="p-2.5 rounded-xl bg-slate-100 shrink-0 mt-0.5">
                      {getNotificationIcon(item.type)}
                    </div>
                    <div className="space-y-1 min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-semibold text-slate-900 truncate">
                          {item.title}
                        </h4>
                        {!item.isRead && (
                          <span className="w-2 h-2 rounded-full bg-brand-600 shrink-0" />
                        )}
                      </div>
                      {item.content && (
                        <p className="text-xs text-slate-600 leading-relaxed max-w-3xl">
                          {item.content}
                        </p>
                      )}
                      <div className="flex items-center gap-3 text-[11px] text-slate-400 font-medium pt-1">
                        <span>{formatRelativeTime(item.createdAt)}</span>
                        <span>•</span>
                        <span>{formatDateTime(item.createdAt)}</span>
                      </div>
                    </div>
                  </div>

                  {!item.isRead && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleMarkOne(item.id)}
                      className="text-xs text-slate-500 hover:text-brand-600 shrink-0"
                    >
                      Mark read
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
