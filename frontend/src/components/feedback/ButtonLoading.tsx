import React from 'react';
import { LoadingSpinner } from './LoadingSpinner';

export interface ButtonLoadingProps {
  loading?: boolean;
  children: React.ReactNode;
  loadingText?: string;
}

export const ButtonLoading: React.FC<ButtonLoadingProps> = ({
  loading = false,
  children,
  loadingText,
}) => {
  if (loading) {
    return (
      <span className="inline-flex items-center gap-2">
        <LoadingSpinner size="sm" />
        <span>{loadingText || children}</span>
      </span>
    );
  }

  return <>{children}</>;
};
