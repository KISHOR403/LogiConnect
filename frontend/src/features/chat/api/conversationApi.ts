import { apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import {
  ConversationItem,
  ConversationResponse,
  ConversationType,
  ConversationMemberResponse,
} from '@/types/conversation';

export interface ConversationFilterParams {
  type?: ConversationType;
  search?: string;
  page?: number;
  size?: number;
}

export const conversationApi = {
  async getConversations(params: ConversationFilterParams = {}): Promise<PageResponse<ConversationItem>> {
    const { page = 0, size = 10, type, search } = params;
    const response = await apiClient.get<PageResponse<ConversationResponse>>('/conversations', {
      params: {
        page,
        size,
        ...(type ? { type } : {}),
        ...(search ? { search } : {}),
      },
    });
    return response.data;
  },

  async getConversationById(id: string): Promise<ConversationItem> {
    const response = await apiClient.get<ConversationResponse>(`/conversations/${id}`);
    return response.data;
  },

  async getConversationMembers(id: string): Promise<ConversationMemberResponse[]> {
    const response = await apiClient.get<ConversationMemberResponse[]>(`/conversations/${id}/members`);
    return response.data;
  },

  async createDirectConversation(participantId: string): Promise<ConversationItem> {
    const response = await apiClient.post<ConversationResponse>('/conversations/direct', {
      participantId,
    });
    return response.data;
  },
};
