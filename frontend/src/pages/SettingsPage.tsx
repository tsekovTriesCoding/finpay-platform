import { useState, useCallback } from 'react';
import { ArrowLeft, User, Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

import { useAuth } from '../contexts/AuthContext';
import { DashboardHeader } from '../components/dashboard';
import { ProfileTab, NotificationPreferencesTab } from '../components/settings';

type SettingsTab = 'profile' | 'notifications';

const TABS: { id: SettingsTab; label: string; icon: React.ReactNode }[] = [
  { id: 'profile', label: 'Profile', icon: <User className="w-4 h-4" /> },
  { id: 'notifications', label: 'Notifications', icon: <Bell className="w-4 h-4" /> },
];

export default function SettingsPage() {
  const { user, logout, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<SettingsTab>('profile');

  const handleProfileUpdated = useCallback(async () => {
    await refreshUser();
  }, [refreshUser]);

  if (!user) return null;

  return (
    <div className="min-h-screen bg-dark-950">
      <DashboardHeader user={user} onLogout={logout} />

      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <button
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-2 text-dark-400 hover:text-white transition-colors mb-4"
          >
            <ArrowLeft className="w-4 h-4" />
            <span className="text-sm">Back to Dashboard</span>
          </button>

          <h1 className="text-2xl font-bold text-white">Settings</h1>
          <p className="text-dark-400 mt-1">
            Manage your profile and notification preferences
          </p>
        </div>

        <div className="flex gap-1 bg-dark-800/50 border border-dark-700/50 rounded-xl p-1 mb-8">
          {TABS.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`relative flex items-center gap-2 flex-1 px-4 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === tab.id
                  ? 'text-white'
                  : 'text-dark-400 hover:text-dark-200'
              }`}
            >
              {activeTab === tab.id && (
                <motion.div
                  layoutId="activeTab"
                  className="absolute inset-0 bg-dark-700/80 rounded-lg"
                  transition={{ type: 'spring', bounce: 0.2, duration: 0.4 }}
                />
              )}
              <span className="relative z-10 flex items-center gap-2">
                {tab.icon}
                {tab.label}
              </span>
            </button>
          ))}
        </div>

        <AnimatePresence mode="wait">
          {activeTab === 'profile' && (
            <ProfileTab
              key="profile"
              user={user}
              onProfileUpdated={handleProfileUpdated}
            />
          )}
          {activeTab === 'notifications' && (
            <NotificationPreferencesTab
              key="notifications"
              userId={user.id}
            />
          )}
        </AnimatePresence>
      </main>
    </div>
  );
}
