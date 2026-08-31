import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, MessageSquare } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ROUTES } from '@/lib/constants/routes';

export const ConversationDetailPage: React.FC = () => {
  const { conversationId } = useParams<{ conversationId: string }>();

  return (
    <div className="space-y-4">
      <Link
        to={ROUTES.MESSAGES}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-700"
      >
        <ArrowLeft size={14} /> Back to Messages
      </Link>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Conversation Details</CardTitle>
          <p className="text-xs text-slate-500 font-mono">ID: {conversationId}</p>
        </CardHeader>
        <CardContent className="p-8">
          <EmptyState
            icon={MessageSquare}
            title="Conversation Room"
            description="Message history and thread view will be rendered in the upcoming Messaging feature module."
          />
        </CardContent>
      </Card>
    </div>
  );
};
