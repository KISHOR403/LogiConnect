import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, Home, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/lib/constants/routes';

export const UnauthorizedPage: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 text-center bg-slate-50 text-slate-900">
      <div className="w-16 h-16 rounded-2xl bg-amber-50 flex items-center justify-center text-amber-600 mb-4 shadow-sm border border-amber-200">
        <ShieldAlert size={32} />
      </div>
      <h1 className="text-3xl font-bold tracking-tight text-slate-900">Access Restricted</h1>
      <p className="text-sm text-slate-600 max-w-md mt-2 mb-6">
        You do not have the required administrative role or permission to view this section of LogiConnect.
      </p>

      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => window.history.back()} leftIcon={<ArrowLeft size={14} />}>
          Go Back
        </Button>
        <Link to={ROUTES.DASHBOARD}>
          <Button variant="primary" size="sm" leftIcon={<Home size={14} />}>
            Return to Dashboard
          </Button>
        </Link>
      </div>
    </div>
  );
};
