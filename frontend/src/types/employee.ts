import { Role } from './auth';

export type EmployeeStatus = 'ACTIVE' | 'PROBATION' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED' | 'RESIGNED';

export interface EmployeeSummary {
  id: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  name: string;
  email: string;
  phone?: string;
  designation?: string;
  location?: string;
  status: EmployeeStatus;
  departmentId?: string;
  departmentName?: string;
  teamId?: string;
  teamName?: string;
  managerId?: string;
  managerName?: string;
  roles?: Role[];
  avatarUrl?: string;
  isOnline?: boolean;
  presence?: 'online' | 'offline' | 'busy' | 'away';
  joiningDate?: string;
}

export interface EmployeeResponse extends EmployeeSummary {}

export interface EmployeeFilterParams {
  departmentId?: string;
  teamId?: string;
  status?: string;
  location?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}
