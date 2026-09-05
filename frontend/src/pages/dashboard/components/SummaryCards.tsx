import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MessageSquare, Megaphone, Calendar, Users, ArrowRight } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/feedback/Skeleton';
import { conversationApi } from '@/features/chat/api/conversationApi';
import { announcementApi } from '@/features/announcements/api/announcementApi';
import { employeeApi } from '@/features/employees/api/employeeApi';
import { meetingApi } from '@/features/meetings/api/meetingApi';
import { ROUTES } from '@/lib/constants/routes';

interface SummaryData {
  conversationCount: number | null;
  announcementCount: number | null;
  meetingCount: number | null;
  employeeCount: number | null;
}

export const SummaryCards: React.FC = () => {
  const [data, setData] = useState<SummaryData>({
    conversationCount: null,
    announcementCount: null,
    meetingCount: null,
    employeeCount: null,
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    let isMounted = true;

    const fetchSummaryMetrics = async () => {
      setIsLoading(true);
      const [convRes, annRes, empRes, meetRes] = await Promise.allSettled([
        conversationApi.getConversations({ size: 1 }),
        announcementApi.getAnnouncements({ size: 1, status: 'PUBLISHED' }),
        employeeApi.getEmployees({ size: 1 }),
        meetingApi.getUpcomingMeetings({ size: 1 }),
      ]);

      if (!isMounted) return;

      setData({
        conversationCount:
          convRes.status === 'fulfilled' ? convRes.value.totalElements ?? convRes.value.content?.length ?? 0 : 0,
        announcementCount:
          annRes.status === 'fulfilled' ? annRes.value.totalElements ?? annRes.value.content?.length ?? 0 : 0,
        employeeCount:
          empRes.status === 'fulfilled' ? empRes.value.totalElements ?? 2000 : 2000,
        meetingCount:
          meetRes.status === 'fulfilled' ? meetRes.value.totalElements ?? meetRes.value.content?.length ?? 0 : 0,
      });

      setIsLoading(false);
    };

    fetchSummaryMetrics();

    return () => {
      isMounted = false;
    };
  }, []);

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i} className="p-4 space-y-3">
            <Skeleton variant="circular" width={36} height={36} />
            <Skeleton variant="text" width="60%" height={16} />
            <Skeleton variant="text" width="40%" height={12} />
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
      {/* Direct Messages Card */}
      <Link to={ROUTES.MESSAGES} className="group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded-xl">
        <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
          <div>
            <div className="p-2.5 rounded-lg bg-blue-50 text-blue-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <MessageSquare size={20} />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
              Direct Messages
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {data.conversationCount !== null && data.conversationCount > 0
                ? `${data.conversationCount} active thread${data.conversationCount > 1 ? 's' : ''}`
                : 'Direct & team chat'}
            </p>
          </div>
          <div className="mt-3 flex items-center text-[11px] font-medium text-brand-600 group-hover:text-brand-700">
            <span>Open conversations</span>
            <ArrowRight size={12} className="ml-1 group-hover:translate-x-0.5 transition-transform" />
          </div>
        </Card>
      </Link>

      {/* Announcements Card */}
      <Link to={ROUTES.ANNOUNCEMENTS} className="group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded-xl">
        <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
          <div>
            <div className="p-2.5 rounded-lg bg-amber-50 text-amber-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <Megaphone size={20} />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
              Announcements
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {data.announcementCount !== null && data.announcementCount > 0
                ? `${data.announcementCount} official update${data.announcementCount > 1 ? 's' : ''}`
                : 'Company & shift memos'}
            </p>
          </div>
          <div className="mt-3 flex items-center text-[11px] font-medium text-brand-600 group-hover:text-brand-700">
            <span>View updates</span>
            <ArrowRight size={12} className="ml-1 group-hover:translate-x-0.5 transition-transform" />
          </div>
        </Card>
      </Link>

      {/* Meetings Card */}
      <Link to={ROUTES.MEETINGS} className="group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded-xl">
        <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
          <div>
            <div className="p-2.5 rounded-lg bg-purple-50 text-purple-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <Calendar size={20} />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
              Meetings
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {data.meetingCount !== null && data.meetingCount > 0
                ? `${data.meetingCount} upcoming sync${data.meetingCount > 1 ? 's' : ''}`
                : 'Operations briefings'}
            </p>
          </div>
          <div className="mt-3 flex items-center text-[11px] font-medium text-brand-600 group-hover:text-brand-700">
            <span>View calendar</span>
            <ArrowRight size={12} className="ml-1 group-hover:translate-x-0.5 transition-transform" />
          </div>
        </Card>
      </Link>

      {/* Directory Card */}
      <Link to={ROUTES.EMPLOYEES} className="group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded-xl">
        <Card className="p-4 hover:border-brand-300 hover:shadow-md transition-all h-full flex flex-col justify-between">
          <div>
            <div className="p-2.5 rounded-lg bg-emerald-50 text-emerald-600 w-fit mb-3 group-hover:scale-105 transition-transform">
              <Users size={20} />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
              Directory
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {data.employeeCount !== null
                ? `${data.employeeCount.toLocaleString()}+ employees`
                : '2,000+ employees'}
            </p>
          </div>
          <div className="mt-3 flex items-center text-[11px] font-medium text-brand-600 group-hover:text-brand-700">
            <span>Find colleague</span>
            <ArrowRight size={12} className="ml-1 group-hover:translate-x-0.5 transition-transform" />
          </div>
        </Card>
      </Link>
    </div>
  );
};
