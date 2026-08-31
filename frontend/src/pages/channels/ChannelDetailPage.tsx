import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Hash } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ROUTES } from '@/lib/constants/routes';

export const ChannelDetailPage: React.FC = () => {
  const { channelId } = useParams<{ channelId: string }>();

  return (
    <div className="space-y-4">
      <Link
        to={ROUTES.CHANNELS}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-700"
      >
        <ArrowLeft size={14} /> Back to Channels
      </Link>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Channel View</CardTitle>
          <p className="text-xs text-slate-500 font-mono">ID: {channelId}</p>
        </CardHeader>
        <CardContent className="p-8">
          <EmptyState
            icon={Hash}
            title="Channel Stream"
            description="Channel broadcast messages, pinned updates, and members will be loaded here."
          />
        </CardContent>
      </Card>
    </div>
  );
};
