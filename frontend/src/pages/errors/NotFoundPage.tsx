import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion, ArrowLeft, Home } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/lib/constants/routes';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 text-center bg-slate-50 text-slate-900">
      <div className="w-16 h-16 rounded-2xl bg-brand-50 flex items-center justify-center text-brand-600 mb-4 shadow-sm border border-brand-100">
        <FileQuestion size={32} />
      </div>
      <h1 className="text-3xl font-bold tracking-tight text-slate-900">Page Not Found</h1>
      <p className="text-sm text-slate-500 max-w-md mt-2 mb-6">
        The resource or route you are looking for does not exist or has been relocated within the operations portal.
      </p>

      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => window.history.back()} leftIcon={<ArrowLeft size={14} />}>
          Go Back
        </Button>
        <Link to={ROUTES.DASHBOARD}>
          <Button variant="primary" size="sm" leftIcon={<Home size={14} />}>
            Operations Dashboard
          </Button>
        </Link>
      </div>
    </div>
  );
};
