import React from 'react';
import { MessageSquare, Plus, Search } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/feedback/EmptyState';

export const MessagesPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Direct & Group Messages</h1>
          <p className="text-sm text-slate-500">Secure real-time communication with colleagues and operations teams.</p>
        </div>
        <Button variant="primary" size="sm" leftIcon={<Plus size={15} />}>
          New Conversation
        </Button>
      </div>

      <Card>
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex items-center gap-3">
            <div className="relative flex-1 max-w-sm">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder="Filter conversations..."
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-100 rounded-lg border-transparent focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-12">
          <EmptyState
            icon={MessageSquare}
            title="No conversations yet"
            description="Select a colleague from the Directory or start a new group discussion to begin messaging."
            actionLabel="Start New Conversation"
          />
        </CardContent>
      </Card>
    </div>
  );
};

export const ConversationDetailPage: React.FC = () => {
  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Conversation</CardTitle>
        </CardHeader>
        <CardContent className="p-8">
          <EmptyState
            icon={MessageSquare}
            title="Conversation loaded"
            description="Message history and thread view will be rendered in the Messaging module step."
          />
        </CardContent>
      </Card>
    </div>
  );
};
