import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { DashboardHeader } from '@/pages/dashboard/components/DashboardHeader';
import { QuickActions } from '@/pages/dashboard/components/QuickActions';
import { RecentAnnouncements } from '@/pages/dashboard/components/RecentAnnouncements';
import { RecentConversations } from '@/pages/dashboard/components/RecentConversations';
import { UpcomingMeetings } from '@/pages/dashboard/components/UpcomingMeetings';
import { PriorityAlerts } from '@/pages/dashboard/components/PriorityAlerts';
import { SearchBar } from '@/components/common/SearchBar';
import { announcementApi } from '@/features/announcements/api/announcementApi';
import { conversationApi } from '@/features/chat/api/conversationApi';
import { meetingApi } from '@/features/meetings/api/meetingApi';
import { searchService } from '@/services/searchService';
import { CurrentUser } from '@/types/auth';

const mockEmployeeUser: CurrentUser = {
  id: 'emp-001',
  employeeCode: 'EMP20491',
  name: 'Priya Sharma',
  firstName: 'Priya',
  lastName: 'Sharma',
  email: 'priya.sharma@logiconnect.internal',
  status: 'ACTIVE',
  department: {
    id: 'dept-ops',
    code: 'OPS-BLR',
    name: 'Bangalore Hub Operations',
  },
  location: 'Bangalore Warehouse 4',
  roles: ['EMPLOYEE'],
  permissions: ['SEND_MESSAGES'],
};

const mockAdminUser: CurrentUser = {
  id: 'adm-001',
  employeeCode: 'ADM9001',
  name: 'Rajesh Nair',
  firstName: 'Rajesh',
  lastName: 'Nair',
  email: 'rajesh.nair@logiconnect.internal',
  status: 'ACTIVE',
  department: {
    id: 'dept-hr',
    code: 'HR-HQ',
    name: 'People Operations',
  },
  roles: ['SUPER_ADMIN'],
  permissions: ['ALL_PERMISSIONS'],
};

describe('Dashboard Component Architecture & Enterprise Features', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  describe('1. DashboardHeader', () => {
    it('renders real authenticated employee data without hardcoding', () => {
      render(
        <MemoryRouter>
          <DashboardHeader user={mockEmployeeUser} />
        </MemoryRouter>
      );

      expect(screen.getByText(/welcome back, priya/i)).toBeInTheDocument();
      expect(screen.getByText('EMP20491')).toBeInTheDocument();
      expect(screen.getByText('Bangalore Hub Operations')).toBeInTheDocument();
      expect(screen.getByText('Bangalore Warehouse 4')).toBeInTheDocument();
      expect(screen.getByText('Employee')).toBeInTheDocument();
    });

    it('renders skeleton loader when user is loading', () => {
      const { container } = render(
        <MemoryRouter>
          <DashboardHeader user={null} isLoading={true} />
        </MemoryRouter>
      );

      expect(container.querySelector('.animate-pulse')).toBeInTheDocument();
    });
  });

  describe('2. QuickActions & Role Awareness', () => {
    it('shows standard employee actions for regular employee', () => {
      render(
        <MemoryRouter>
          <QuickActions user={mockEmployeeUser} />
        </MemoryRouter>
      );

      expect(screen.getByRole('button', { name: /new message/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /schedule sync/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /manage access/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /onboard employee/i })).not.toBeInTheDocument();
    });

    it('shows administrative actions for SUPER_ADMIN', () => {
      render(
        <MemoryRouter>
          <QuickActions user={mockAdminUser} />
        </MemoryRouter>
      );

      expect(screen.getByRole('button', { name: /manage access/i })).toBeInTheDocument();
    });
  });

  describe('3. RecentAnnouncements', () => {
    it('renders live announcements when data is returned', async () => {
      vi.spyOn(announcementApi, 'getAnnouncements').mockResolvedValueOnce({
        content: [
          {
            id: 'ann-1',
            title: 'Bangalore Hub Shift Timing Change',
            content: 'Morning shift starts at 07:00 AM.',
            summary: 'Morning shift adjusted by 30 minutes.',
            type: 'DEPARTMENT',
            status: 'PUBLISHED',
            targetScope: 'DEPARTMENT_ONLY',
            targetDepartmentName: 'Bangalore Hub',
            isMandatoryAcknowledgement: true,
            isAcknowledged: false,
            createdAt: new Date().toISOString(),
            authorId: 'auth-1',
            authorName: 'Suresh Kumar',
          },
        ],
        pageNumber: 0,
        pageSize: 4,
        totalElements: 1,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(
        <MemoryRouter>
          <RecentAnnouncements />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Bangalore Hub Shift Timing Change')).toBeInTheDocument();
        expect(screen.getByText(/sign-off required/i)).toBeInTheDocument();
      });
    });

    it('renders intentional human empty state when no announcements exist', async () => {
      vi.spyOn(announcementApi, 'getAnnouncements').mockResolvedValueOnce({
        content: [],
        pageNumber: 0,
        pageSize: 4,
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        empty: true,
      });

      render(
        <MemoryRouter>
          <RecentAnnouncements />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText("You're all caught up")).toBeInTheDocument();
        expect(screen.getByText(/new company and operational updates will appear here/i)).toBeInTheDocument();
      });
    });

    it('renders localized error state with retry on failure', async () => {
      vi.spyOn(announcementApi, 'getAnnouncements').mockRejectedValueOnce(new Error('Network error'));

      render(
        <MemoryRouter>
          <RecentAnnouncements />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /unable to load announcements/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
      });
    });
  });

  describe('4. RecentConversations', () => {
    it('renders live conversations with unread badge and avatar', async () => {
      vi.spyOn(conversationApi, 'getConversations').mockResolvedValueOnce({
        content: [
          {
            id: 'conv-1',
            type: 'DIRECT',
            participants: [
              {
                userId: 'user-2',
                employeeCode: 'EMP1002',
                name: 'Rahul Sharma',
                isOnline: true,
                status: 'online',
              },
            ],
            lastMessage: {
              id: 'msg-1',
              senderId: 'user-2',
              senderName: 'Rahul Sharma',
              content: 'Vehicle 102 is ready for loading at Dock 4.',
              sentAt: new Date().toISOString(),
            },
            unreadCount: 2,
            updatedAt: new Date().toISOString(),
            createdAt: new Date().toISOString(),
          },
        ],
        pageNumber: 0,
        pageSize: 4,
        totalElements: 1,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(
        <MemoryRouter>
          <RecentConversations />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
        expect(screen.getByText(/vehicle 102 is ready for loading/i)).toBeInTheDocument();
        expect(screen.getByText('2')).toBeInTheDocument();
      });
    });

    it('renders empty state when no conversations exist', async () => {
      vi.spyOn(conversationApi, 'getConversations').mockResolvedValueOnce({
        content: [],
        pageNumber: 0,
        pageSize: 4,
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        empty: true,
      });

      render(
        <MemoryRouter>
          <RecentConversations />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/no recent conversations/i)).toBeInTheDocument();
      });
    });
  });

  describe('5. UpcomingMeetings & PriorityAlerts Empty States', () => {
    it('renders professional empty state for meetings', async () => {
      vi.spyOn(meetingApi, 'getUpcomingMeetings').mockResolvedValueOnce({
        content: [],
        pageNumber: 0,
        pageSize: 3,
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        empty: true,
      });

      render(
        <MemoryRouter>
          <UpcomingMeetings />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/no upcoming meetings/i)).toBeInTheDocument();
      });
    });

    it('renders professional empty state for priority alerts', async () => {
      vi.spyOn(announcementApi, 'getAnnouncements').mockResolvedValueOnce({
        content: [],
        pageNumber: 0,
        pageSize: 3,
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        empty: true,
      });

      render(
        <MemoryRouter>
          <PriorityAlerts />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/no priority updates/i)).toBeInTheDocument();
      });
    });
  });

  describe('6. SearchBar Global Search', () => {
    it('triggers global search and displays categorized results', async () => {
      const user = userEvent.setup();

      vi.spyOn(searchService, 'performGlobalSearch').mockResolvedValueOnce({
        people: [
          {
            id: 'p-1',
            title: 'Rahul Sharma',
            subtitle: 'Operations Manager • Logistics Hub',
            category: 'people',
            url: '/app/employees/p-1',
            badge: 'EMP1002',
          },
        ],
        channels: [],
        announcements: [],
        messages: [],
        documents: [],
        meetings: [],
      });

      render(
        <MemoryRouter>
          <SearchBar />
        </MemoryRouter>
      );

      const input = screen.getByPlaceholderText(/search people, messages/i);
      await user.type(input, 'Rahul');

      await waitFor(() => {
        expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
        expect(screen.getByText('EMP1002')).toBeInTheDocument();
      });
    });
  });
});
