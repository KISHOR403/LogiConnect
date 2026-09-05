export type MeetingStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface MeetingItem {
  id: string;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  departmentName?: string;
  teamName?: string;
  organizerName: string;
  organizerId: string;
  participantCount: number;
  location?: string;
  meetingUrl?: string;
  isOnline: boolean;
  status: MeetingStatus;
}
