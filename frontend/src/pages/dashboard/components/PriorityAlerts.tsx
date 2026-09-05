import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Clock, ShieldAlert } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/feedback/Skeleton';
import { announcementApi } from '@/features/announcements/api/announcementApi';
import { AnnouncementItem } from '@/types/announcement';
import { formatRelativeTime } from '@/lib/utils/formatDate';
import { ROUTES } from '@/lib/constants/routes';

export const PriorityAlerts: React.FC = () => {
  const [alerts, setAlerts] = useState<AnnouncementItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchPriorityAlerts = useCallback(async () => {
    setIsLoading(true);
    try {
      // Fetch emergency/high-priority announcements
      const res = await announcementApi.getAnnouncements({
        type: 'EMERGENCY',
        status: 'PUBLISHED',
        size: 3,
      });
      setAlerts(res.content || []);
    } catch {
      setAlerts([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPriorityAlerts();
  }, [fetchPriorityAlerts]);

  return (
    <Card className="border-amber-200/80 bg-gradient-to-br from-amber-50/30 to-white shadow-xs">
      <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-amber-100/60">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center">
            <ShieldAlert size={16} />
          </div>
          <div>
            <CardTitle className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
              Priority Operational Updates
            </CardTitle>
          </div>
        </div>
        <Link
          to={ROUTES.ANNOUNCEMENTS}
          className="text-xs font-medium text-amber-700 hover:text-amber-800 flex items-center gap-1"
        >
          View all <ArrowRight size={13} />
        </Link>
      </CardHeader>

      <CardContent className="pt-3 pb-3">
        {isLoading ? (
          <div className="space-y-3 py-1">
            <Skeleton variant="text" width="80%" height={16} />
            <Skeleton variant="text" width="60%" height={12} />
          </div>
        ) : alerts.length === 0 ? (
          <div className="py-4 text-center">
            <p className="text-xs font-medium text-slate-700">No priority updates</p>
            <p className="text-[11px] text-slate-400 mt-0.5">Important operational alerts will appear here.</p>
          </div>
        ) : (
          <div className="divide-y divide-amber-100/60">
            {alerts.map((alert) => (
              <Link
                key={alert.id}
                to={ROUTES.ANNOUNCEMENT_DETAIL(alert.id)}
                className="block py-2.5 first:pt-1 last:pb-1 group hover:bg-amber-50/50 rounded-lg px-2 -mx-2 transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-0.5 min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold text-slate-900 group-hover:text-amber-800 truncate">
                        {alert.title}
                      </span>
                      <Badge variant="warning" size="sm" className="text-[10px] py-0">
                        Urgent
                      </Badge>
                    </div>
                    {alert.summary && (
                      <p className="text-[11px] text-slate-600 line-clamp-1">{alert.summary}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-1 text-[10px] text-slate-400 font-medium shrink-0">
                    <Clock size={11} />
                    <span>{formatRelativeTime(alert.publishedAt || alert.createdAt)}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
