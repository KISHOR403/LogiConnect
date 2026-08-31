import React from 'react';
import { LoadingSpinner } from './LoadingSpinner';
import { cn } from '@/lib/utils/cn';

export interface PageLoaderProps {
  message?: string;
  className?: string;
  fullScreen?: boolean;
}

export const PageLoader: React.FC<PageLoaderProps> = ({
  message = 'Loading LogiConnect...',
  className,
  fullScreen = true,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 text-slate-500',
        fullScreen ? 'fixed inset-0 z-50 bg-white/80 backdrop-blur-xs' : 'py-16 w-full',
        className
      )}
      role="status"
      aria-live="polite"
    >
      <div className="relative flex items-center justify-center">
        <LoadingSpinner size="lg" className="text-brand-600" />
      </div>
      {message && <p className="text-sm font-medium text-slate-600 animate-pulse">{message}</p>}
    </div>
  );
};
