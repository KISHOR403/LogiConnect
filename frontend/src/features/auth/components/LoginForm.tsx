import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { User, Lock, Eye, EyeOff, AlertCircle, Truck } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { formatApiError } from '@/lib/api/errors';
import { ROUTES } from '@/lib/constants/routes';

export const LoginForm: React.FC = () => {
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || ROUTES.DASHBOARD;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setFieldErrors({});

    const newFieldErrors: Record<string, string> = {};
    if (!usernameOrEmail.trim()) {
      newFieldErrors.usernameOrEmail = 'Username or email is required';
    }
    if (!password) {
      newFieldErrors.password = 'Password is required';
    }

    if (Object.keys(newFieldErrors).length > 0) {
      setFieldErrors(newFieldErrors);
      return;
    }

    try {
      await login({ usernameOrEmail: usernameOrEmail.trim(), password });
      navigate(from, { replace: true });
    } catch (err) {
      const formatted = formatApiError(err);
      setErrorMessage(formatted.message);
      if (formatted.fieldErrors) {
        setFieldErrors(formatted.fieldErrors);
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Mobile Brand Header */}
      <div className="flex lg:hidden items-center gap-2 mb-6">
        <div className="w-10 h-10 rounded-xl bg-brand-600 flex items-center justify-center text-white shadow-md">
          <Truck size={22} />
        </div>
        <div>
          <h2 className="text-xl font-bold text-slate-900">LogiConnect</h2>
          <p className="text-xs text-slate-500">Enterprise Operations Platform</p>
        </div>
      </div>

      <div className="space-y-1.5">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Sign in to your account</h2>
        <p className="text-sm text-slate-500">
          Enter your company credentials to access operational channels and messaging.
        </p>
      </div>

      {errorMessage && (
        <div
          className="flex items-start gap-3 p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs animate-in fade-in duration-150"
          role="alert"
        >
          <AlertCircle size={18} className="text-red-600 shrink-0 mt-0.5" />
          <div className="flex-1 font-medium">{errorMessage}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Input
          label="Username or Corporate Email"
          type="text"
          id="usernameOrEmail"
          autoComplete="username"
          required
          placeholder="e.g. jdoe or john.doe@logiconnect.internal"
          value={usernameOrEmail}
          onChange={(e) => setUsernameOrEmail(e.target.value)}
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
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          leftIcon={<Lock size={16} />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowPassword((prev) => !prev)}
              className="p-1 text-slate-400 hover:text-slate-600 focus:outline-none"
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
            className="w-full font-semibold shadow-sm"
          >
            Sign In
          </Button>
        </div>
      </form>

      <div className="pt-4 border-t border-slate-100 text-center">
        <p className="text-xs text-slate-500">
          Trouble signing in? Contact the <span className="font-semibold text-slate-700">IT Helpdesk</span> at Ext. 4400.
        </p>
        <p className="text-[11px] text-slate-400 mt-2">
          This system is restricted to authorized employees. All activity is logged and audited.
        </p>
      </div>
    </div>
  );
};
