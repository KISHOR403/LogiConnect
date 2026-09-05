import React from 'react';
import { AlertCircle, Lock, ShieldAlert, WifiOff, ServerCrash } from 'lucide-react';
import { AuthErrorDetails, AuthErrorCode } from '../utils/authErrors';

interface AuthErrorAlertProps {
  error: AuthErrorDetails | null;
  className?: string;
}

export const AuthErrorAlert: React.FC<AuthErrorAlertProps> = ({ error, className = '' }) => {
  if (!error) return null;

  const getIcon = (code: AuthErrorCode) => {
    switch (code) {
      case 'ACCOUNT_LOCKED':
        return <Lock size={18} className="text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />;
      case 'ACCOUNT_INACTIVE':
        return <ShieldAlert size={18} className="text-red-600 shrink-0 mt-0.5" aria-hidden="true" />;
      case 'NETWORK_ERROR':
        return <WifiOff size={18} className="text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />;
      case 'SERVER_ERROR':
        return <ServerCrash size={18} className="text-red-600 shrink-0 mt-0.5" aria-hidden="true" />;
      case 'INVALID_CREDENTIALS':
      default:
        return <AlertCircle size={18} className="text-red-600 shrink-0 mt-0.5" aria-hidden="true" />;
    }
  };

  const getContainerStyles = (code: AuthErrorCode) => {
    switch (code) {
      case 'ACCOUNT_LOCKED':
      case 'NETWORK_ERROR':
        return 'bg-amber-50 border-amber-200 text-amber-900';
      case 'ACCOUNT_INACTIVE':
      case 'SERVER_ERROR':
      case 'INVALID_CREDENTIALS':
      default:
        return 'bg-red-50 border-red-200 text-red-900';
    }
  };

  return (
    <div
      role="alert"
      aria-live="polite"
      className={`flex items-start gap-3 p-3.5 rounded-xl border text-xs leading-relaxed ${getContainerStyles(
        error.code
      )} ${className}`}
    >
      {getIcon(error.code)}
      <div className="flex-1">
        <p className="font-semibold">{error.title}</p>
        <p className="mt-0.5 text-xs opacity-90">{error.message}</p>
      </div>
    </div>
  );
};
