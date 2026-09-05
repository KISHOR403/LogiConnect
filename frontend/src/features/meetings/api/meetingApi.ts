import { apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import { MeetingItem } from '@/types/meeting';

export interface MeetingFilterParams {
  page?: number;
  size?: number;
  upcomingOnly?: boolean;
}

/**
 * Service client for operational meetings and team syncs.
 * TODO: Connect to backend `/meetings` endpoint once meeting scheduling microservice is provisioned.
 */
export const meetingApi = {
  async getUpcomingMeetings(params: MeetingFilterParams = {}): Promise<PageResponse<MeetingItem>> {
    const { page = 0, size = 5 } = params;
    try {
      const response = await apiClient.get<PageResponse<MeetingItem>>('/meetings', {
        params: { page, size, upcomingOnly: true },
      });
      return response.data;
    } catch {
      // Graceful fallback for empty schedule when backend endpoint is not yet deployed
      return {
        content: [],
        pageNumber: page,
        pageSize: size,
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        empty: true,
      };
    }
  },

  async getMeetingById(id: string): Promise<MeetingItem | null> {
    try {
      const response = await apiClient.get<MeetingItem>(`/meetings/${id}`);
      return response.data;
    } catch {
      return null;
    }
  },
};
