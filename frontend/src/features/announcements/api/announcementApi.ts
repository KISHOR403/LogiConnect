import { apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import {
  AnnouncementItem,
  AnnouncementResponse,
  AnnouncementType,
  AnnouncementStatus,
  AnnouncementReadResponse,
} from '@/types/announcement';

export interface AnnouncementFilterParams {
  type?: AnnouncementType;
  status?: AnnouncementStatus;
  search?: string;
  page?: number;
  size?: number;
}

export const announcementApi = {
  async getAnnouncements(params: AnnouncementFilterParams = {}): Promise<PageResponse<AnnouncementItem>> {
    const { page = 0, size = 10, type, status, search } = params;
    const response = await apiClient.get<PageResponse<AnnouncementResponse>>('/announcements', {
      params: {
        page,
        size,
        ...(type ? { type } : {}),
        ...(status ? { status } : {}),
        ...(search ? { search } : {}),
      },
    });
    return response.data;
  },

  async getAnnouncementById(id: string): Promise<AnnouncementItem> {
    const response = await apiClient.get<AnnouncementResponse>(`/announcements/${id}`);
    return response.data;
  },

  async markAsRead(id: string): Promise<AnnouncementReadResponse> {
    const response = await apiClient.post<AnnouncementReadResponse>(`/announcements/${id}/read`);
    return response.data;
  },

  async acknowledgeAnnouncement(id: string): Promise<AnnouncementReadResponse> {
    const response = await apiClient.post<AnnouncementReadResponse>(`/announcements/${id}/acknowledge`);
    return response.data;
  },
};
