export type NotificationType =
  | 'MESSAGE'
  | 'GROUP_MESSAGE'
  | 'CHANNEL_MESSAGE'
  | 'ANNOUNCEMENT'
  | 'URGENT_ANNOUNCEMENT'
  | 'ACKNOWLEDGEMENT_REQUIRED'
  | 'MEETING_INVITATION'
  | 'MEETING_UPDATED'
  | 'MEETING_CANCELLED'
  | 'MENTION'
  | 'SECURITY';

export interface NotificationItem {
  id: string;
  type: NotificationType;
  referenceType?: string;
  referenceId?: string;
  title: string;
  content?: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
