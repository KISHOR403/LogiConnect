export type AnnouncementType = 'COMPANY' | 'DEPARTMENT' | 'TEAM' | 'EMERGENCY';
export type AnnouncementStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'CANCELLED' | 'ARCHIVED';
export type TargetScope = 'ALL_EMPLOYEES' | 'DEPARTMENT_ONLY' | 'TEAM_ONLY';

export interface AnnouncementItem {
  id: string;
  title: string;
  content: string;
  summary?: string;
  type: AnnouncementType;
  status: AnnouncementStatus;
  targetScope: TargetScope;
  targetDepartmentId?: string;
  targetTeamId?: string;
  targetDepartmentName?: string;
  targetTeamName?: string;
  isMandatoryAcknowledgement: boolean;
  publishedAt?: string;
  scheduledPublishAt?: string;
  createdAt: string;
  authorId: string;
  authorName: string;
  isRead?: boolean;
  isAcknowledged?: boolean;
}

export interface AnnouncementResponse extends AnnouncementItem {}

export interface AnnouncementReadResponse {
  announcementId: string;
  userId: string;
  readAt: string;
  acknowledgedAt?: string;
}

export interface AcknowledgementReportResponse {
  announcementId: string;
  totalEligible: number;
  readCount: number;
  acknowledgedCount: number;
  readPercentage: number;
  acknowledgementPercentage: number;
}
