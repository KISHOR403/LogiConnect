import React from 'react';
import { Inbox } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils/cn';

export interface EmptyStateProps {
  icon?: React.ComponentType<{ className?: string; size?: number | string }>;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
  compact?: boolean;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon = Inbox,
  title,
  description,
  actionLabel,
  onAction,
  className,
  compact = false,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-center rounded-xl border border-dashed border-slate-200 bg-slate-50/60',
        compact ? 'p-6' : 'p-10',
        className
      )}
    >
      <div
        className={cn(
          'rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-400 shadow-xs mb-3',
          compact ? 'w-10 h-10' : 'w-14 h-14'
        )}
      >
        <Icon size={compact ? 20 : 26} aria-hidden="true" />
      </div>
      <h4 className={cn('font-semibold text-slate-800', compact ? 'text-sm' : 'text-base')}>
        {title}
      </h4>
      {description && (
        <p className={cn('text-slate-500 max-w-sm mt-1', compact ? 'text-xs' : 'text-sm')}>
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <Button size="sm" variant="primary" onClick={onAction} className="mt-4">
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
