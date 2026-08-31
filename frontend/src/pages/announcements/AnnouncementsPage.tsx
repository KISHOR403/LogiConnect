import React from 'react';
import { Megaphone, Plus, Search, Filter } from 'lucide-react';
import { Card, CardHeader, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/feedback/EmptyState';

export const AnnouncementsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Official Announcements</h1>
          <p className="text-sm text-slate-500">Corporate broadcasts, policy memos, shift alerts, and compliance notices.</p>
        </div>
        <Button variant="primary" size="sm" leftIcon={<Plus size={15} />}>
          Draft Announcement
        </Button>
      </div>

      <Card>
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex items-center justify-between gap-3">
            <div className="relative flex-1 max-w-sm">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder="Search announcements..."
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-100 rounded-lg border-transparent focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
            <Button variant="outline" size="sm" leftIcon={<Filter size={14} />}>
              Filter Scope
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-12">
          <EmptyState
            icon={Megaphone}
            title="No announcements published"
            description="You have no active company or department announcements pending review."
          />
        </CardContent>
      </Card>
    </div>
  );
};
