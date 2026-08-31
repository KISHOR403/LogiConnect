import React, { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { KeyRound, Bell, Sliders, Check, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { authApi } from '@/features/auth/api/authApi';
import { formatApiError } from '@/lib/api/errors';
import { cn } from '@/lib/utils/cn';

export const SettingsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'general';

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(false);

    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirmation do not match');
      return;
    }

    if (newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters long');
      return;
    }

    setIsChangingPassword(true);
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      setPasswordSuccess(true);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setPasswordError(formatApiError(err).message);
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Application Settings</h1>
        <p className="text-sm text-slate-500">Manage security credentials, notification alerts, and application preferences.</p>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          onClick={() => setSearchParams({ tab: 'general' })}
          className={cn(
            'pb-3 text-xs font-semibold uppercase tracking-wider transition-colors border-b-2 flex items-center gap-2 focus-ring',
            activeTab === 'general'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          )}
        >
          <Sliders size={14} /> General
        </button>

        <button
          onClick={() => setSearchParams({ tab: 'password' })}
          className={cn(
            'pb-3 text-xs font-semibold uppercase tracking-wider transition-colors border-b-2 flex items-center gap-2 focus-ring',
            activeTab === 'password'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          )}
        >
          <KeyRound size={14} /> Password & Security
        </button>

        <button
          onClick={() => setSearchParams({ tab: 'notifications' })}
          className={cn(
            'pb-3 text-xs font-semibold uppercase tracking-wider transition-colors border-b-2 flex items-center gap-2 focus-ring',
            activeTab === 'notifications'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-900'
          )}
        >
          <Bell size={14} /> Alerts
        </button>
      </div>

      {/* Password Change Form */}
      {activeTab === 'password' && (
        <Card>
          <CardHeader>
            <CardTitle>Change Password</CardTitle>
            <CardDescription>
              Ensure your account is using a strong password of at least 8 characters.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {passwordSuccess && (
              <div className="p-3.5 mb-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2 font-medium">
                <Check size={16} className="text-emerald-600" />
                Password changed successfully.
              </div>
            )}

            {passwordError && (
              <div className="p-3.5 mb-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-center gap-2 font-medium">
                <AlertCircle size={16} className="text-red-600" />
                {passwordError}
              </div>
            )}

            <form onSubmit={handlePasswordSubmit} className="space-y-4 max-w-md">
              <Input
                label="Current Password"
                type="password"
                required
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />

              <Input
                label="New Password"
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                helperText="Must be at least 8 characters with letters and numbers."
              />

              <Input
                label="Confirm New Password"
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />

              <div className="pt-2">
                <Button type="submit" variant="primary" isLoading={isChangingPassword}>
                  Update Password
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* General Settings */}
      {activeTab === 'general' && (
        <Card>
          <CardHeader>
            <CardTitle>Display & Regional Settings</CardTitle>
            <CardDescription>Configure interface language, timezone, and operational view mode.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between p-4 rounded-lg bg-slate-50 border border-slate-200">
              <div>
                <p className="text-sm font-semibold text-slate-800">Timezone</p>
                <p className="text-xs text-slate-500">UTC (Coordinated Universal Time)</p>
              </div>
              <span className="text-xs font-mono text-slate-600 bg-white px-2.5 py-1 rounded border border-slate-200">
                UTC
              </span>
            </div>

            <div className="flex items-center justify-between p-4 rounded-lg bg-slate-50 border border-slate-200">
              <div>
                <p className="text-sm font-semibold text-slate-800">Language</p>
                <p className="text-xs text-slate-500">English (United States)</p>
              </div>
              <span className="text-xs font-medium text-slate-600 bg-white px-2.5 py-1 rounded border border-slate-200">
                English
              </span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Alert Settings */}
      {activeTab === 'notifications' && (
        <Card>
          <CardHeader>
            <CardTitle>In-App Notification Preferences</CardTitle>
            <CardDescription>Manage real-time in-app badges and sound cues.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between p-4 rounded-lg bg-slate-50 border border-slate-200">
              <div>
                <p className="text-sm font-semibold text-slate-800">Direct Message Alerts</p>
                <p className="text-xs text-slate-500">Show red badge when colleagues message you</p>
              </div>
              <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                Enabled
              </span>
            </div>

            <div className="flex items-center justify-between p-4 rounded-lg bg-slate-50 border border-slate-200">
              <div>
                <p className="text-sm font-semibold text-slate-800">Urgent Broadcast Alerts</p>
                <p className="text-xs text-slate-500">High-priority company notices</p>
              </div>
              <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                Enabled
              </span>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};
