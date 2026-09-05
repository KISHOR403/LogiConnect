import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Megaphone, ArrowRight, ShieldCheck, Clock, Building } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/feedback/Skeleton';
import { ErrorState } from '@/components/feedback/ErrorState';
import { EmptyState } from '@/components/feedback/EmptyState';
import { announcementApi } from '@/features/announcements/api/announcementApi';
import { AnnouncementItem } from '@/types/announcement';
import { formatRelativeTime } from '@/lib/utils/formatDate';
import { ROUTES } from '@/lib/constants/routes';

export const RecentAnnouncements: React.FC = () => {
  const [announcements, setAnnouncements] = useState<AnnouncementItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAnnouncements = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await announcementApi.getAnnouncements({
        status: 'PUBLISHED',
        size: 4,
      });
      setAnnouncements(response.content || []);
    } catch {
      setError('Unable to load announcements.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnnouncements();
  }, [fetchAnnouncements]);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-100">
        <div>
          <CardTitle className="text-base flex items-center gap-2">
            <Megaphone size={17} className="text-amber-500" />
            <span>Latest Official Announcements</span>
          </CardTitle>
        </div>
        <Link
          to={ROUTES.ANNOUNCEMENTS}
          className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-500 rounded"
        >
          <span>View all</span>
          <ArrowRight size={13} />
        </Link>
      </CardHeader>

      <CardContent className="pt-4">
        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-2 py-2">
                <Skeleton variant="text" width="65%" height={16} />
                <Skeleton variant="text" width="40%" height={12} />
              </div>
            ))}
          </div>
        ) : error ? (
          <ErrorState
            title="Unable to load announcements"
            message={error}
            onRetry={fetchAnnouncements}
          />
        ) : announcements.length === 0 ? (
          <EmptyState
            compact
            icon={Megaphone}
            title="You're all caught up"
            description="New company and operational updates will appear here."
          />
        ) : (
          <div className="divide-y divide-slate-100">
            {announcements.map((item) => (
              <Link
                key={item.id}
                to={ROUTES.ANNOUNCEMENT_DETAIL(item.id)}
                className="block py-3 first:pt-0 last:pb-0 group hover:bg-slate-50/80 -mx-3 px-3 rounded-lg transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-1 min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs sm:text-sm font-semibold text-slate-800 group-hover:text-brand-600 truncate">
                        {item.title}
                      </h4>
                      {item.isMandatoryAcknowledgement && !item.isAcknowledged && (
                        <Badge variant="warning" size="sm" className="text-[10px] py-0 shrink-0">
                          <ShieldCheck size={10} className="mr-0.5" />
                          Sign-off Required
                        </Badge>
                      )}
                    </div>
                    {item.summary && (
                      <p className="text-xs text-slate-500 line-clamp-1">{item.summary}</p>
                    )}
                    <div className="flex items-center gap-2 text-[11px] text-slate-400">
                      {item.targetDepartmentName ? (
                        <span className="flex items-center gap-1">
                          <Building size={11} />
                          {item.targetDepartmentName}
                        </span>
                      ) : (
                        <span>Company Broadcast</span>
                      )}
                      <span>•</span>
                      <span>By {item.authorName || 'Operations'}</span>
                    </div>
                  </div>
                  <span className="text-[10px] text-slate-400 font-medium whitespace-nowrap shrink-0 flex items-center gap-1 mt-0.5">
                    <Clock size={11} />
                    {formatRelativeTime(item.publishedAt || item.createdAt)}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
