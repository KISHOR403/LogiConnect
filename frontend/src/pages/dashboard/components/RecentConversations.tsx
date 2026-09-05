import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { MessageSquare, ArrowRight, Hash, Users } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Avatar } from '@/components/common/Avatar';
import { Skeleton } from '@/components/feedback/Skeleton';
import { ErrorState } from '@/components/feedback/ErrorState';
import { EmptyState } from '@/components/feedback/EmptyState';
import { conversationApi } from '@/features/chat/api/conversationApi';
import { ConversationItem } from '@/types/conversation';
import { formatRelativeTime } from '@/lib/utils/formatDate';
import { ROUTES } from '@/lib/constants/routes';

export const RecentConversations: React.FC = () => {
  const [conversations, setConversations] = useState<ConversationItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchConversations = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await conversationApi.getConversations({ size: 4 });
      setConversations(response.content || []);
    } catch {
      setError('Unable to load recent conversations.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConversations();
  }, [fetchConversations]);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-100">
        <div>
          <CardTitle className="text-base flex items-center gap-2">
            <MessageSquare size={17} className="text-blue-500" />
            <span>Recent Conversations</span>
          </CardTitle>
        </div>
        <Link
          to={ROUTES.MESSAGES}
          className="text-xs font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-500 rounded"
        >
          <span>Open chat</span>
          <ArrowRight size={13} />
        </Link>
      </CardHeader>

      <CardContent className="pt-4">
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex items-center gap-3 py-2">
                <Skeleton variant="circular" width={38} height={38} />
                <div className="space-y-1.5 flex-1">
                  <Skeleton variant="text" width="50%" height={14} />
                  <Skeleton variant="text" width="75%" height={12} />
                </div>
              </div>
            ))}
          </div>
        ) : error ? (
          <ErrorState
            title="Unable to load conversations"
            message={error}
            onRetry={fetchConversations}
          />
        ) : conversations.length === 0 ? (
          <EmptyState
            compact
            icon={MessageSquare}
            title="No recent conversations"
            description="Your recent messages will appear here."
            actionLabel="Start Conversation"
            onAction={() => {
              window.location.href = ROUTES.MESSAGES;
            }}
          />
        ) : (
          <div className="divide-y divide-slate-100">
            {conversations.map((conv) => {
              const primaryParticipant = conv.participants?.[0];
              const title =
                conv.title ||
                conv.name ||
                (conv.participants?.length > 1
                  ? conv.participants.map((p) => p.name).join(', ')
                  : primaryParticipant?.name || 'Direct Chat');

              const isChannel = conv.type === 'CHANNEL';
              const isGroup = conv.type === 'GROUP';
              const presenceStatus = primaryParticipant?.status || (primaryParticipant?.isOnline ? 'online' : 'offline');

              return (
                <Link
                  key={conv.id}
                  to={isChannel ? ROUTES.CHANNEL_DETAIL(conv.id) : ROUTES.MESSAGE_DETAIL(conv.id)}
                  className="flex items-center gap-3 py-3 first:pt-0 last:pb-0 group hover:bg-slate-50/80 -mx-3 px-3 rounded-lg transition-colors"
                >
                  {isChannel ? (
                    <div className="w-9 h-9 rounded-full bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                      <Hash size={18} />
                    </div>
                  ) : isGroup ? (
                    <div className="w-9 h-9 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                      <Users size={18} />
                    </div>
                  ) : (
                    <Avatar
                      name={title}
                      src={primaryParticipant?.avatarUrl || conv.avatarUrl}
                      size="md"
                      status={presenceStatus}
                    />
                  )}

                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-2">
                      <h4 className="text-xs sm:text-sm font-semibold text-slate-800 group-hover:text-brand-600 truncate">
                        {isChannel ? `#${title}` : title}
                      </h4>
                      <span className="text-[10px] text-slate-400 font-medium shrink-0">
                        {formatRelativeTime(conv.lastMessage?.sentAt || conv.updatedAt || conv.createdAt)}
                      </span>
                    </div>

                    <div className="flex items-center justify-between gap-2 mt-0.5">
                      <p className="text-xs text-slate-500 truncate">
                        {conv.lastMessage ? (
                          <>
                            <span className="font-medium text-slate-700">
                              {conv.lastMessage.senderName ? `${conv.lastMessage.senderName.split(' ')[0]}: ` : ''}
                            </span>
                            {conv.lastMessage.content}
                          </>
                        ) : (
                          <span className="text-slate-400 italic">No messages yet</span>
                        )}
                      </p>

                      {conv.unreadCount !== undefined && conv.unreadCount > 0 && (
                        <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-full bg-brand-600 text-white shrink-0">
                          {conv.unreadCount}
                        </span>
                      )}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
