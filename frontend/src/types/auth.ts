export type Role = 'SUPER_ADMIN' | 'HR_ADMIN' | 'MANAGER' | 'TEAM_LEADER' | 'EMPLOYEE';

export interface DepartmentSummary {
  id: string;
  code: string;
  name: string;
}

export interface TeamSummary {
  id: string;
  code: string;
  name: string;
}

export interface UserSummary {
  id: string;
  username: string;
  name: string;
  email: string;
  roles: Role[];
}

export interface CurrentUser {
  id: string;
  employeeCode: string;
  name: string;
  firstName: string;
  lastName: string;
  email: string;
  designation?: string;
  location?: string;
  status: 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED';
  department?: DepartmentSummary;
  team?: TeamSummary;
  roles: Role[];
  permissions: string[];
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthState {
  user: CurrentUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
