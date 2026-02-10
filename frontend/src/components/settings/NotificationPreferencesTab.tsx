import { useState, useEffect, useCallback } from 'react';
import {
  Bell,
  Mail,
  MessageSquare,
  Smartphone,
  Monitor,
  CreditCard,
  Shield,
  Megaphone,
  Settings,
  Loader2,
  CheckCircle,
  Save,
} from 'lucide-react';
import { motion } from 'framer-motion';

import type {
  NotificationPreferences,
  NotificationPreferencesRequest,
} from '../../api/notificationApi';
import { notificationService } from '../../api/notificationApi';

interface NotificationPreferencesTabProps {
  userId: string;
}

interface ToggleProps {
  enabled: boolean;
  onChange: (value: boolean) => void;
  label: string;
  description: string;
  icon: React.ReactNode;
}

function Toggle({ enabled, onChange, label, description, icon }: ToggleProps) {
  return (
    <div className="flex items-center justify-between py-4 border-b border-dark-700/50 last:border-0">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 text-dark-400">{icon}</div>
        <div>
          <p className="text-sm font-medium text-white">{label}</p>
          <p className="text-xs text-dark-400 mt-0.5">{description}</p>
        </div>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={enabled}
        onClick={() => onChange(!enabled)}
        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500/50 ${
          enabled ? 'bg-primary-600' : 'bg-dark-600'
        }`}
      >
        <span
          className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform duration-200 ${
            enabled ? 'translate-x-6' : 'translate-x-1'
          }`}
        />
      </button>
    </div>
  );
}

export default function NotificationPreferencesTab({ userId }: NotificationPreferencesTabProps) {
  const [prefs, setPrefs] = useState<NotificationPreferencesRequest>({
    emailEnabled: true,
    smsEnabled: false,
    pushEnabled: true,
    inAppEnabled: true,
    paymentNotifications: true,
    securityNotifications: true,
    promotionalNotifications: false,
    systemNotifications: true,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasChanges, setHasChanges] = useState(false);
  const [originalPrefs, setOriginalPrefs] = useState<NotificationPreferencesRequest | null>(null);

  useEffect(() => {
    const fetchPreferences = async () => {
      try {
        const data: NotificationPreferences = await notificationService.getPreferences(userId);
        const mapped: NotificationPreferencesRequest = {
          emailEnabled: data.emailEnabled,
          smsEnabled: data.smsEnabled,
          pushEnabled: data.pushEnabled,
          inAppEnabled: data.inAppEnabled,
          paymentNotifications: data.paymentNotifications,
          securityNotifications: data.securityNotifications,
          promotionalNotifications: data.promotionalNotifications,
          systemNotifications: data.systemNotifications,
        };
        setPrefs(mapped);
        setOriginalPrefs(mapped);
      } catch {
        setError('Failed to load notification preferences');
      } finally {
        setIsLoading(false);
      }
    };

    fetchPreferences();
  }, [userId]);

  useEffect(() => {
    if (originalPrefs) {
      const changed = (Object.keys(prefs) as (keyof NotificationPreferencesRequest)[]).some(
        key => prefs[key] !== originalPrefs[key],
      );
      setHasChanges(changed);
    }
  }, [prefs, originalPrefs]);

  const updatePref = useCallback(
    (key: keyof NotificationPreferencesRequest, value: boolean) => {
      setPrefs(prev => ({ ...prev, [key]: value }));
      setSaveSuccess(false);
    },
    [],
  );

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    setSaveSuccess(false);

    try {
      await notificationService.updatePreferences(userId, prefs);
      setOriginalPrefs({ ...prefs });
      setHasChanges(false);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch {
      setError('Failed to save notification preferences. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-8 h-8 text-primary-400 animate-spin" />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-6"
    >
      <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
          <Bell className="w-5 h-5 text-primary-400" />
          Delivery Channels
        </h3>
        <p className="text-sm text-dark-400 mb-4">
          Choose how you want to receive notifications
        </p>

        <div>
          <Toggle
            enabled={prefs.emailEnabled}
            onChange={v => updatePref('emailEnabled', v)}
            label="Email Notifications"
            description="Receive notifications via email for important account activities"
            icon={<Mail className="w-5 h-5" />}
          />
          <Toggle
            enabled={prefs.smsEnabled}
            onChange={v => updatePref('smsEnabled', v)}
            label="SMS Notifications"
            description="Get text messages for critical alerts and security notifications"
            icon={<MessageSquare className="w-5 h-5" />}
          />
          <Toggle
            enabled={prefs.pushEnabled}
            onChange={v => updatePref('pushEnabled', v)}
            label="Push Notifications"
            description="Browser push notifications for real-time updates"
            icon={<Smartphone className="w-5 h-5" />}
          />
          <Toggle
            enabled={prefs.inAppEnabled}
            onChange={v => updatePref('inAppEnabled', v)}
            label="In-App Notifications"
            description="Show notifications inside the FinPay dashboard"
            icon={<Monitor className="w-5 h-5" />}
          />
        </div>
      </div>

      <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
          <Settings className="w-5 h-5 text-primary-400" />
          Notification Types
        </h3>
        <p className="text-sm text-dark-400 mb-4">
          Control which types of notifications you receive
        </p>

        <div>
          <Toggle
            enabled={prefs.paymentNotifications}
            onChange={v => updatePref('paymentNotifications', v)}
            label="Payment Notifications"
            description="Transfers, payments, money requests, and bill payment updates"
            icon={<CreditCard className="w-5 h-5" />}
          />
          <Toggle
            enabled={prefs.securityNotifications}
            onChange={v => updatePref('securityNotifications', v)}
            label="Security Notifications"
            description="Login alerts, password changes, and suspicious activity"
            icon={<Shield className="w-5 h-5" />}
          />
          <Toggle
            enabled={prefs.promotionalNotifications}
            onChange={v => updatePref('promotionalNotifications', v)}
            label="Promotional Notifications"
            description="Offers, new features, and product announcements"
            icon={<Megaphone className="w-5 h-5" />}
          />
          <Toggle
            enabled={prefs.systemNotifications}
            onChange={v => updatePref('systemNotifications', v)}
            label="System Notifications"
            description="Maintenance updates, policy changes, and service alerts"
            icon={<Settings className="w-5 h-5" />}
          />
        </div>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3 text-red-400 text-sm">
          {error}
        </div>
      )}

      {saveSuccess && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-green-500/10 border border-green-500/20 rounded-xl px-4 py-3 text-green-400 text-sm flex items-center gap-2"
        >
          <CheckCircle className="w-4 h-4" />
          Notification preferences saved successfully! Changes propagated via event stream.
        </motion.div>
      )}

      <div className="flex justify-end">
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleSave}
          disabled={isSaving || !hasChanges}
          className="flex items-center gap-2 px-6 py-2.5 bg-primary-600 hover:bg-primary-500 disabled:bg-primary-600/50 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-colors shadow-lg shadow-primary-500/25"
        >
          {isSaving ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Saving...
            </>
          ) : (
            <>
              <Save className="w-4 h-4" />
              {hasChanges ? 'Save Preferences' : 'No Changes'}
            </>
          )}
        </motion.button>
      </div>
    </motion.div>
  );
}
