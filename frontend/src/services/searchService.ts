import { employeeApi } from '@/features/employees/api/employeeApi';
import { announcementApi } from '@/features/announcements/api/announcementApi';
import { conversationApi } from '@/features/chat/api/conversationApi';
import { GroupedSearchResults, SearchResultItem } from '@/types/search';
import { ROUTES } from '@/lib/constants/routes';

export const searchService = {
  async performGlobalSearch(query: string): Promise<GroupedSearchResults> {
    const trimmed = query.trim();
    if (!trimmed || trimmed.length < 2) {
      return {
        people: [],
        channels: [],
        announcements: [],
        messages: [],
        documents: [],
        meetings: [],
      };
    }

    // Query available backend endpoints concurrently
    const [employeesRes, announcementsRes, conversationsRes] = await Promise.allSettled([
      employeeApi.getEmployees({ search: trimmed, size: 5 }),
      announcementApi.getAnnouncements({ search: trimmed, size: 5 }),
      conversationApi.getConversations({ search: trimmed, size: 5 }),
    ]);

    const people: SearchResultItem[] = [];
    if (employeesRes.status === 'fulfilled' && employeesRes.value.content) {
      employeesRes.value.content.forEach((emp) => {
        people.push({
          id: emp.id,
          title: emp.name || `${emp.firstName} ${emp.lastName}`,
          subtitle: emp.designation ? `${emp.designation} • ${emp.departmentName || 'Operations'}` : emp.departmentName,
          category: 'people',
          url: ROUTES.EMPLOYEE_DETAIL(emp.id),
          badge: emp.employeeCode,
        });
      });
    }

    const announcements: SearchResultItem[] = [];
    if (announcementsRes.status === 'fulfilled' && announcementsRes.value.content) {
      announcementsRes.value.content.forEach((ann) => {
        announcements.push({
          id: ann.id,
          title: ann.title,
          subtitle: ann.summary || ann.targetDepartmentName || 'Company Broadcast',
          category: 'announcements',
          url: ROUTES.ANNOUNCEMENT_DETAIL(ann.id),
          badge: ann.type,
        });
      });
    }

    const messages: SearchResultItem[] = [];
    const channels: SearchResultItem[] = [];
    if (conversationsRes.status === 'fulfilled' && conversationsRes.value.content) {
      conversationsRes.value.content.forEach((conv) => {
        if (conv.type === 'CHANNEL') {
          channels.push({
            id: conv.id,
            title: conv.title || conv.name || 'Operations Channel',
            subtitle: conv.lastMessage?.content,
            category: 'channels',
            url: ROUTES.CHANNEL_DETAIL(conv.id),
          });
        } else {
          messages.push({
            id: conv.id,
            title: conv.title || conv.name || conv.participants.map((p) => p.name).join(', ') || 'Conversation',
            subtitle: conv.lastMessage?.content ? `"${conv.lastMessage.content}"` : 'Direct message',
            category: 'messages',
            url: ROUTES.MESSAGE_DETAIL(conv.id),
            badge: conv.unreadCount && conv.unreadCount > 0 ? `${conv.unreadCount} unread` : undefined,
          });
        }
      });
    }

    return {
      people,
      channels,
      announcements,
      messages,
      documents: [],
      meetings: [],
    };
  },
};
