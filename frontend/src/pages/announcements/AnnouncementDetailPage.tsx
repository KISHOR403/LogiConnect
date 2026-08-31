import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Megaphone } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ROUTES } from '@/lib/constants/routes';

export const AnnouncementDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  return (
    <div className="space-y-4">
      <Link
        to={ROUTES.ANNOUNCEMENTS}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-700"
      >
        <ArrowLeft size={14} /> Back to Announcements
      </Link>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Announcement Details</CardTitle>
          <p className="text-xs text-slate-500 font-mono">ID: {id}</p>
        </CardHeader>
        <CardContent className="p-8">
          <EmptyState
            icon={Megaphone}
            title="Official Broadcast"
            description="Full announcement body, target audience info, and acknowledgement submission will be displayed here."
          />
        </CardContent>
      </Card>
    </div>
  );
};
