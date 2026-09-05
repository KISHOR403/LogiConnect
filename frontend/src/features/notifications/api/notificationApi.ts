import { apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import { NotificationItem, UnreadCountResponse } from '@/types/notification';

export const notificationApi = {
  async getNotifications(page = 0, size = 20): Promise<PageResponse<NotificationItem>> {
    const response = await apiClient.get<PageResponse<NotificationItem>>('/notifications', {
      params: { page, size },
    });
    return response.data;
  },

  async getUnreadCount(): Promise<number> {
    try {
      const response = await apiClient.get<UnreadCountResponse>('/notifications/unread-count');
      return response.data?.unreadCount ?? 0;
    } catch {
      return 0;
    }
  },

  async markAsRead(notificationId: string): Promise<NotificationItem> {
    const response = await apiClient.post<NotificationItem>(`/notifications/${notificationId}/read`);
    return response.data;
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.post('/notifications/read-all');
  },
};
