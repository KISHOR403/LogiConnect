import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CheckCheck, Megaphone, MessageSquare, AlertCircle, Calendar, Shield } from 'lucide-react';
import { notificationApi } from '@/features/notifications/api/notificationApi';
import { NotificationItem, NotificationType } from '@/types/notification';
import { formatRelativeTime } from '@/lib/utils/formatDate';
import { EmptyState } from '@/components/feedback/EmptyState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useOnClickOutside } from '@/hooks/useOnClickOutside';
import { ROUTES } from '@/lib/constants/routes';
import { cn } from '@/lib/utils/cn';

function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case 'ANNOUNCEMENT':
    case 'URGENT_ANNOUNCEMENT':
    case 'ACKNOWLEDGEMENT_REQUIRED':
      return <Megaphone size={15} className="text-amber-600" />;
    case 'MESSAGE':
    case 'GROUP_MESSAGE':
    case 'CHANNEL_MESSAGE':
      return <MessageSquare size={15} className="text-blue-600" />;
    case 'MEETING_INVITATION':
    case 'MEETING_UPDATED':
    case 'MEETING_CANCELLED':
      return <Calendar size={15} className="text-purple-600" />;
    case 'SECURITY':
      return <Shield size={15} className="text-rose-600" />;
    default:
      return <AlertCircle size={15} className="text-slate-600" />;
  }
}

export const NotificationDropdown: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useOnClickOutside(dropdownRef, () => setIsOpen(false), isOpen);

  const fetchUnreadCount = async () => {
    try {
      const count = await notificationApi.getUnreadCount();
      setUnreadCount(count);
    } catch {
      // Graceful fallback if backend is offline or starting
    }
  };

  const fetchNotifications = async () => {
    setIsLoading(true);
    try {
      const response = await notificationApi.getNotifications(0, 10);
      setNotifications(response.content || []);
    } catch {
      setNotifications([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleToggle = () => {
    if (!isOpen) {
      fetchNotifications();
      fetchUnreadCount();
    }
    setIsOpen((prev) => !prev);
  };

  const handleMarkAsRead = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // Handled silently
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
      );
      setUnreadCount(0);
    } catch {
      // Handled silently
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={handleToggle}
        className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors focus-ring"
        aria-label="Notifications"
        aria-expanded={isOpen}
      >
        <Bell size={19} />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 flex h-4 min-w-[16px] items-center justify-center px-1 text-[10px] font-bold text-white bg-rose-500 rounded-full ring-2 ring-white animate-in zoom-in-50 duration-150">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-slate-200 py-0 z-50 overflow-hidden animate-in fade-in slide-in-from-top-1 duration-150"
          role="dialog"
          aria-label="Notifications Panel"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/50">
            <div className="flex items-center gap-2">
              <h4 className="text-sm font-semibold text-slate-900">Notifications</h4>
              {unreadCount > 0 && (
                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-rose-100 text-rose-700 rounded-full">
                  {unreadCount} new
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="inline-flex items-center gap-1 text-[11px] font-medium text-brand-600 hover:text-brand-700 hover:underline"
              >
                <CheckCheck size={13} />
                Mark all read
              </button>
            )}
          </div>

          {/* List */}
          <div className="max-h-[380px] overflow-y-auto divide-y divide-slate-100">
            {isLoading ? (
              <div className="py-12 flex justify-center items-center">
                <LoadingSpinner size="md" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-6">
                <EmptyState
                  compact
                  icon={Bell}
                  title="No notifications"
                  description="You're all caught up! New alerts will show up here."
                />
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  className={cn(
                    'p-3.5 hover:bg-slate-50 transition-colors flex gap-3 items-start relative group',
                    !item.isRead ? 'bg-brand-50/30' : ''
                  )}
                >
                  <div className="p-2 rounded-lg bg-slate-100 shrink-0 mt-0.5">
                    {getNotificationIcon(item.type)}
                  </div>
                  <div className="flex-1 min-w-0 pr-4">
                    <p className="text-xs font-semibold text-slate-800 leading-snug line-clamp-1">
                      {item.title}
                    </p>
                    {item.content && (
                      <p className="text-xs text-slate-500 mt-0.5 line-clamp-2 leading-relaxed">
                        {item.content}
                      </p>
                    )}
                    <span className="text-[10px] text-slate-400 font-medium mt-1 block">
                      {formatRelativeTime(item.createdAt)}
                    </span>
                  </div>

                  {!item.isRead && (
                    <button
                      onClick={(e) => handleMarkAsRead(item.id, e)}
                      title="Mark as read"
                      className="absolute right-2.5 top-3 p-1 text-slate-400 hover:text-brand-600 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <CheckCheck size={14} />
                    </button>
                  )}
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="p-2.5 text-center border-t border-slate-100 bg-slate-50/50">
            <Link
              to={ROUTES.NOTIFICATIONS}
              onClick={() => setIsOpen(false)}
              className="text-xs font-medium text-brand-600 hover:text-brand-700 hover:underline block py-1"
            >
              View all notifications
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};
