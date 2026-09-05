import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, ArrowRight, Video, Users, Clock } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/feedback/Skeleton';
import { ErrorState } from '@/components/feedback/ErrorState';
import { EmptyState } from '@/components/feedback/EmptyState';
import { meetingApi } from '@/features/meetings/api/meetingApi';
import { MeetingItem } from '@/types/meeting';
import { formatTime } from '@/lib/utils/formatDate';
import { ROUTES } from '@/lib/constants/routes';

export const UpcomingMeetings: React.FC = () => {
  const [meetings, setMeetings] = useState<MeetingItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMeetings = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await meetingApi.getUpcomingMeetings({ size: 3 });
      setMeetings(response.content || []);
    } catch {
      setError('Unable to load upcoming meetings.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMeetings();
  }, [fetchMeetings]);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-100">
        <div>
          <CardTitle className="text-base flex items-center gap-2">
            <Calendar size={17} className="text-purple-500" />
            <span>Upcoming Meetings</span>
          </CardTitle>
        </div>
        <Link
          to={ROUTES.MEETINGS}
          className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-500 rounded"
        >
          <span>Calendar</span>
          <ArrowRight size={13} />
        </Link>
      </CardHeader>

      <CardContent className="pt-4">
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 2 }).map((_, i) => (
              <div key={i} className="p-3 rounded-lg border border-slate-100 space-y-2">
                <Skeleton variant="text" width="60%" height={14} />
                <Skeleton variant="text" width="40%" height={12} />
              </div>
            ))}
          </div>
        ) : error ? (
          <ErrorState
            title="Unable to load meetings"
            message={error}
            onRetry={fetchMeetings}
          />
        ) : meetings.length === 0 ? (
          <EmptyState
            compact
            icon={Calendar}
            title="No upcoming meetings"
            description="Scheduled meetings will appear here."
            actionLabel="Schedule Meeting"
            onAction={() => {
              window.location.href = ROUTES.MEETINGS;
            }}
          />
        ) : (
          <div className="space-y-3">
            {meetings.map((meeting) => (
              <div
                key={meeting.id}
                className="p-3 rounded-xl border border-slate-200/80 bg-slate-50/50 hover:bg-slate-50 transition-colors"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="space-y-1 min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-purple-700">
                      <Clock size={12} />
                      <span>{formatTime(meeting.startTime)}</span>
                      {meeting.endTime && <span> - {formatTime(meeting.endTime)}</span>}
                    </div>

                    <h4 className="text-xs sm:text-sm font-semibold text-slate-900 truncate">
                      {meeting.title}
                    </h4>

                    <div className="flex flex-wrap items-center gap-2 text-[11px] text-slate-500">
                      <span>{meeting.departmentName || 'Operations'}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <Users size={11} />
                        {meeting.participantCount} participants
                      </span>
                    </div>
                  </div>

                  <Link to={ROUTES.MEETING_DETAIL(meeting.id)} className="shrink-0">
                    <Button size="sm" variant="outline" className="text-xs h-7 px-2.5 bg-white">
                      {meeting.isOnline ? (
                        <>
                          <Video size={12} className="mr-1 text-purple-600" />
                          Join
                        </>
                      ) : (
                        'View'
                      )}
                    </Button>
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
