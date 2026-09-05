export type ConversationType = 'DIRECT' | 'GROUP' | 'CHANNEL';

export interface ParticipantSummary {
  userId: string;
  employeeCode: string;
  name: string;
  avatarUrl?: string;
  designation?: string;
  departmentName?: string;
  isOnline?: boolean;
  status?: 'online' | 'offline' | 'busy' | 'away';
}

export interface LastMessageSummary {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  sentAt: string;
  messageType?: 'TEXT' | 'FILE' | 'SYSTEM';
}

export interface ConversationItem {
  id: string;
  type: ConversationType;
  title?: string;
  name?: string;
  avatarUrl?: string;
  lastMessage?: LastMessageSummary;
  participants: ParticipantSummary[];
  unreadCount?: number;
  updatedAt: string;
  createdAt: string;
  isMuted?: boolean;
  isPinned?: boolean;
}

export interface ConversationResponse extends ConversationItem {}

export interface ConversationMemberResponse {
  userId: string;
  name: string;
  employeeCode: string;
  role: 'ADMIN' | 'MEMBER';
  joinedAt: string;
}
