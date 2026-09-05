import React from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { DashboardHeader } from './components/DashboardHeader';
import { QuickActions } from './components/QuickActions';
import { SummaryCards } from './components/SummaryCards';
import { PriorityAlerts } from './components/PriorityAlerts';
import { RecentAnnouncements } from './components/RecentAnnouncements';
import { RecentConversations } from './components/RecentConversations';
import { UpcomingMeetings } from './components/UpcomingMeetings';
import { RecentNotifications } from './components/RecentNotifications';

export const DashboardPage: React.FC = () => {
  const { user, isLoading } = useAuth();

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* 1. Authenticated Employee Welcome Banner */}
      <DashboardHeader user={user} isLoading={isLoading} />

      {/* 2. Permission-Aware Quick Actions Bar */}
      <div className="flex items-center justify-between gap-4">
        <QuickActions user={user} />
      </div>

      {/* 3. Live Metrics Quick Access Cards */}
      <SummaryCards />

      {/* 4. Priority Operational Alerts */}
      <PriorityAlerts />

      {/* 5. Main Dashboard Grid (2 Columns Left, 1 Column Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 Cols): Latest Announcements & Recent Conversations */}
        <div className="lg:col-span-2 space-y-6">
          <RecentAnnouncements />
          <RecentConversations />
        </div>

        {/* Right Column (1 Col): Notifications Feed & Upcoming Meetings */}
        <div className="space-y-6">
          <RecentNotifications />
          <UpcomingMeetings />
        </div>
      </div>
    </div>
  );
};
