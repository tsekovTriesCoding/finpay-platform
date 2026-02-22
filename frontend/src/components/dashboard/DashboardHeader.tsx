import { Settings, LogOut, Zap, Sparkles, Building2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import type { User } from '../../api/authApi';
import { NotificationBell } from '../notifications';

const PLAN_BADGE = {
  STARTER:    { label: 'Starter',    icon: Zap,       className: 'bg-dark-700/60 text-dark-300' },
  PRO:        { label: 'Pro',        icon: Sparkles,  className: 'bg-primary-500/15 text-primary-300' },
  ENTERPRISE: { label: 'Enterprise', icon: Building2,  className: 'bg-secondary-500/15 text-secondary-300' },
} as const;

interface DashboardHeaderProps {
  user: User;
  onLogout: () => Promise<void>;
}

/**
 * Top navigation bar for the dashboard.
 * Presentational - receives user data and logout callback from the parent.
 */
export default function DashboardHeader({ user, onLogout }: DashboardHeaderProps) {
  const navigate = useNavigate();

  const handleLogout = async () => {
    await onLogout();
    navigate('/');
  };

  return (
    <header className="bg-dark-900/50 border-b border-dark-800/50 backdrop-blur-xl sticky top-0 z-[60]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-primary-600 to-primary-500 rounded-lg flex items-center justify-center shadow-lg shadow-primary-500/25">
              <span className="text-white font-bold">F</span>
            </div>
            <span className="text-xl font-bold text-gradient">FinPay</span>
          </div>

          <div className="flex items-center gap-4">
            <NotificationBell userId={user.id} />
            <button
              onClick={() => navigate('/settings')}
              className="p-2 text-dark-400 hover:text-white transition-colors"
              title="Settings"
            >
              <Settings className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full flex items-center justify-center text-white font-medium shadow-lg shadow-primary-500/25 overflow-hidden">
                {user.profileImageUrl ? (
                  <img
                    src={user.profileImageUrl}
                    alt={`${user.firstName} ${user.lastName}`}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full bg-gradient-to-br from-primary-600 to-primary-500 flex items-center justify-center">
                    {user.firstName?.[0]}
                    {user.lastName?.[0]}
                  </div>
                )}
              </div>
              <div className="hidden sm:block">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-white">
                    {user.firstName} {user.lastName}
                  </p>
                  {(() => {
                    const badge = PLAN_BADGE[user.plan];
                    const Icon = badge.icon;
                    return (
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ${badge.className}`}>
                        <Icon className="w-2.5 h-2.5" />
                        {badge.label}
                      </span>
                    );
                  })()}
                </div>
                <p className="text-xs text-dark-400">{user.email}</p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="p-2 text-dark-400 hover:text-red-400 transition-colors"
              title="Logout"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
