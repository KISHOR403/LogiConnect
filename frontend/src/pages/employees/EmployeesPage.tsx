import React from 'react';
import { Users, Search, Filter } from 'lucide-react';
import { Card, CardHeader, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/feedback/EmptyState';

export const EmployeesPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Employee Directory</h1>
          <p className="text-sm text-slate-500">Corporate directory of 2,000+ operations, logistics, and management staff.</p>
        </div>
      </div>

      <Card>
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex items-center justify-between gap-3">
            <div className="relative flex-1 max-w-sm">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder="Search by name, code, designation..."
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-100 rounded-lg border-transparent focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
            <Button variant="outline" size="sm" leftIcon={<Filter size={14} />}>
              Filter Department
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-12">
          <EmptyState
            icon={Users}
            title="Search the directory"
            description="Type a name, employee code, or department above to search corporate staff."
          />
        </CardContent>
      </Card>
    </div>
  );
};
