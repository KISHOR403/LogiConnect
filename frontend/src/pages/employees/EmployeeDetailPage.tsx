import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, User } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ROUTES } from '@/lib/constants/routes';

export const EmployeeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  return (
    <div className="space-y-4">
      <Link
        to={ROUTES.EMPLOYEES}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-700"
      >
        <ArrowLeft size={14} /> Back to Directory
      </Link>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Employee Profile</CardTitle>
          <p className="text-xs text-slate-500 font-mono">ID: {id}</p>
        </CardHeader>
        <CardContent className="p-8">
          <EmptyState
            icon={User}
            title="Employee Record"
            description="Department assignment, team membership, contact info, and direct message shortcuts will appear here."
          />
        </CardContent>
      </Card>
    </div>
  );
};
