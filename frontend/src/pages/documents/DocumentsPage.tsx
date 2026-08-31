import React from 'react';
import { FileText, Upload, Search } from 'lucide-react';
import { Card, CardHeader, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/feedback/EmptyState';

export const DocumentsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Operational Documents</h1>
          <p className="text-sm text-slate-500">Logistics SOPs, safety guidelines, compliance forms, and fleet attachments.</p>
        </div>
        <Button variant="primary" size="sm" leftIcon={<Upload size={15} />}>
          Upload Document
        </Button>
      </div>

      <Card>
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="relative flex-1 max-w-sm">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              placeholder="Search documents and files..."
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-100 rounded-lg border-transparent focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
            />
          </div>
        </CardHeader>
        <CardContent className="p-12">
          <EmptyState
            icon={FileText}
            title="No documents uploaded"
            description="Shared company guidelines, dispatch manuals, and forms will be stored here."
          />
        </CardContent>
      </Card>
    </div>
  );
};
