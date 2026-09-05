import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { User, Lock, Eye, EyeOff, Truck } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/lib/constants/routes';
import { AuthErrorAlert } from './AuthErrorAlert';
import { parseAuthError, AuthErrorDetails } from '../utils/authErrors';

export const LoginForm: React.FC = () => {
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [authError, setAuthError] = useState<AuthErrorDetails | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || ROUTES.DASHBOARD;

  const validate = () => {
    const errors: Record<string, string> = {};
    const trimmed = usernameOrEmail.trim();

    if (!trimmed) {
      errors.usernameOrEmail = 'Username or corporate email is required';
    } else if (trimmed.includes('@')) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(trimmed)) {
        errors.usernameOrEmail = 'Please enter a valid email format';
      }
    }

    if (!password) {
      errors.password = 'Password is required';
    }

    return errors;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    setFieldErrors({});

    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      return;
    }

    try {
      await login({ usernameOrEmail: usernameOrEmail.trim(), password });
      navigate(from, { replace: true });
    } catch (err) {
      const parsed = parseAuthError(err);
      setAuthError(parsed);
      if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setFieldErrors(parsed.fieldErrors);
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Mobile Brand Header */}
      <div className="flex lg:hidden items-center gap-3 mb-6 pb-4 border-b border-slate-100">
        <div className="w-10 h-10 rounded-xl bg-brand-600 flex items-center justify-center text-white shadow-sm">
          <Truck size={22} />
        </div>
        <div>
          <h2 className="text-lg font-bold text-slate-900">LogiConnect</h2>
          <p className="text-xs text-slate-500">Enterprise Internal Communication Platform</p>
        </div>
      </div>

      <div className="space-y-1.5">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Sign in to your account</h2>
        <p className="text-sm text-slate-500">
          Enter your company credentials to access operational channels and messaging.
        </p>
      </div>

      <AuthErrorAlert error={authError} />

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Input
          label="Username or Corporate Email"
          type="text"
          id="usernameOrEmail"
          autoComplete="username"
          required
          placeholder="e.g. EMP1001 or name@logiconnect.internal"
          value={usernameOrEmail}
          onChange={(e) => {
            setUsernameOrEmail(e.target.value);
            if (fieldErrors.usernameOrEmail) {
              setFieldErrors((prev) => ({ ...prev, usernameOrEmail: '' }));
            }
          }}
          error={fieldErrors.usernameOrEmail}
          leftIcon={<User size={16} />}
          disabled={isLoading}
        />

        <Input
          label="Password"
          type={showPassword ? 'text' : 'password'}
          id="password"
          autoComplete="current-password"
          required
          placeholder="••••••••"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            if (fieldErrors.password) {
              setFieldErrors((prev) => ({ ...prev, password: '' }));
            }
          }}
          error={fieldErrors.password}
          leftIcon={<Lock size={16} />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowPassword((prev) => !prev)}
              className="p-1 text-slate-400 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              tabIndex={-1}
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          disabled={isLoading}
        />

        <div className="pt-2">
          <Button
            type="submit"
            variant="primary"
            size="lg"
            isLoading={isLoading}
            disabled={isLoading}
            className="w-full font-semibold shadow-sm"
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </Button>
        </div>
      </form>

      <div className="pt-4 border-t border-slate-100 text-center space-y-2">
        <p className="text-xs text-slate-600">
          Having trouble signing in? Contact your IT administrator.
        </p>
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Authorized employees only. System activity may be logged for security and auditing purposes.
        </p>
      </div>
    </div>
  );
};
