import { apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import { EmployeeSummary, EmployeeResponse, EmployeeFilterParams } from '@/types/employee';

export const employeeApi = {
  async getEmployees(params: EmployeeFilterParams = {}): Promise<PageResponse<EmployeeSummary>> {
    const {
      page = 0,
      size = 20,
      search,
      departmentId,
      teamId,
      status,
      location,
      sort = 'lastName',
      direction = 'asc',
    } = params;

    const response = await apiClient.get<PageResponse<EmployeeResponse>>('/employees', {
      params: {
        page,
        size,
        ...(search ? { search } : {}),
        ...(departmentId ? { departmentId } : {}),
        ...(teamId ? { teamId } : {}),
        ...(status ? { status } : {}),
        ...(location ? { location } : {}),
        sort,
        direction,
      },
    });
    return response.data;
  },

  async getEmployeeById(id: string): Promise<EmployeeSummary> {
    const response = await apiClient.get<EmployeeResponse>(`/employees/${id}`);
    return response.data;
  },
};
