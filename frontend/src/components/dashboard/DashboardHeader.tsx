import { Settings, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import type { User } from '../../api/authApi';

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
    <header className="bg-dark-900/50 border-b border-dark-800/50 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-primary-600 to-primary-500 rounded-lg flex items-center justify-center shadow-lg shadow-primary-500/25">
              <span className="text-white font-bold">F</span>
            </div>
            <span className="text-xl font-bold text-gradient">FinPay</span>
          </div>

          <div className="flex items-center gap-4">
            <button className="p-2 text-dark-400 hover:text-white transition-colors">
              <Settings className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-primary-600 to-primary-500 rounded-full flex items-center justify-center text-white font-medium shadow-lg shadow-primary-500/25">
                {user.firstName?.[0]}
                {user.lastName?.[0]}
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-medium text-white">
                  {user.firstName} {user.lastName}
                </p>
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
