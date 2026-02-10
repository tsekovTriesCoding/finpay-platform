import { useState, useEffect, useCallback } from 'react';
import { Save, Loader2, CheckCircle, User as UserIcon } from 'lucide-react';
import { motion } from 'framer-motion';

import type { User } from '../../api/authApi';
import type { UpdateProfileRequest } from '../../api/userApi';
import { userService } from '../../api/userApi';
import ProfileImageUpload from './ProfileImageUpload';

interface ProfileTabProps {
  user: User;
  onProfileUpdated: () => Promise<void>;
}

export default function ProfileTab({ user, onProfileUpdated }: ProfileTabProps) {
  const [form, setForm] = useState<UpdateProfileRequest>({
    email: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    address: '',
    city: '',
    country: '',
    postalCode: '',
  });
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setForm({
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      phoneNumber: user.phoneNumber ?? '',
      profileImageUrl: user.profileImageUrl,
      address: user.address ?? '',
      city: user.city ?? '',
      country: user.country ?? '',
      postalCode: user.postalCode ?? '',
    });
  }, [user]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleImageUpdated = useCallback(
    async (url: string | null) => {
      setForm(prev => ({ ...prev, profileImageUrl: url }));
      await onProfileUpdated();
    },
    [onProfileUpdated],
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setError(null);
    setSaveSuccess(false);

    try {
      await userService.updateProfile(user.id, form);
      await onProfileUpdated();
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to update profile';
      setError(message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="space-y-8"
    >
      <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <UserIcon className="w-5 h-5 text-primary-400" />
          Profile Photo
        </h3>
        <ProfileImageUpload
          user={{ ...user, profileImageUrl: form.profileImageUrl ?? user.profileImageUrl }}
          onImageUpdated={handleImageUpdated}
        />
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
          <h3 className="text-lg font-semibold text-white mb-6">Personal Information</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-dark-300 mb-1.5">
                First Name
              </label>
              <input
                id="firstName"
                name="firstName"
                type="text"
                value={form.firstName}
                onChange={handleChange}
                required
                minLength={2}
                maxLength={50}
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>

            <div>
              <label htmlFor="lastName" className="block text-sm font-medium text-dark-300 mb-1.5">
                Last Name
              </label>
              <input
                id="lastName"
                name="lastName"
                type="text"
                value={form.lastName}
                onChange={handleChange}
                required
                minLength={2}
                maxLength={50}
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-dark-300 mb-1.5">
                Email Address
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                required
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>

            <div>
              <label htmlFor="phoneNumber" className="block text-sm font-medium text-dark-300 mb-1.5">
                Phone Number
              </label>
              <input
                id="phoneNumber"
                name="phoneNumber"
                type="tel"
                value={form.phoneNumber ?? ''}
                onChange={handleChange}
                placeholder="+1 (555) 000-0000"
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>
          </div>
        </div>

        <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
          <h3 className="text-lg font-semibold text-white mb-6">Address</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label htmlFor="address" className="block text-sm font-medium text-dark-300 mb-1.5">
                Street Address
              </label>
              <input
                id="address"
                name="address"
                type="text"
                value={form.address ?? ''}
                onChange={handleChange}
                placeholder="123 Main Street"
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>

            <div>
              <label htmlFor="city" className="block text-sm font-medium text-dark-300 mb-1.5">
                City
              </label>
              <input
                id="city"
                name="city"
                type="text"
                value={form.city ?? ''}
                onChange={handleChange}
                placeholder="New York"
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>

            <div>
              <label htmlFor="country" className="block text-sm font-medium text-dark-300 mb-1.5">
                Country
              </label>
              <input
                id="country"
                name="country"
                type="text"
                value={form.country ?? ''}
                onChange={handleChange}
                placeholder="United States"
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>

            <div>
              <label htmlFor="postalCode" className="block text-sm font-medium text-dark-300 mb-1.5">
                Postal Code
              </label>
              <input
                id="postalCode"
                name="postalCode"
                type="text"
                value={form.postalCode ?? ''}
                onChange={handleChange}
                placeholder="10001"
                className="w-full px-4 py-2.5 bg-dark-900/50 border border-dark-700 rounded-xl text-white placeholder-dark-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500 transition-all"
              />
            </div>
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
            Profile updated successfully!
          </motion.div>
        )}

        <div className="flex justify-end">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            type="submit"
            disabled={isSaving}
            className="flex items-center gap-2 px-6 py-2.5 bg-primary-600 hover:bg-primary-500 disabled:bg-primary-600/50 text-white font-medium rounded-xl transition-colors shadow-lg shadow-primary-500/25"
          >
            {isSaving ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                Save Changes
              </>
            )}
          </motion.button>
        </div>
      </form>

      <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Account Details</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-dark-400">Status</span>
            <p className="text-white mt-0.5">
              <span
                className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  user.status === 'ACTIVE'
                    ? 'bg-green-500/10 text-green-400'
                    : user.status === 'PENDING_VERIFICATION'
                      ? 'bg-yellow-500/10 text-yellow-400'
                      : 'bg-red-500/10 text-red-400'
                }`}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    user.status === 'ACTIVE'
                      ? 'bg-green-400'
                      : user.status === 'PENDING_VERIFICATION'
                        ? 'bg-yellow-400'
                        : 'bg-red-400'
                  }`}
                />
                {user.status.replace('_', ' ')}
              </span>
            </p>
          </div>
          <div>
            <span className="text-dark-400">Role</span>
            <p className="text-white mt-0.5">{user.role}</p>
          </div>
          <div>
            <span className="text-dark-400">Email Verified</span>
            <p className={`mt-0.5 ${user.emailVerified ? 'text-green-400' : 'text-yellow-400'}`}>
              {user.emailVerified ? 'Verified' : 'Not verified'}
            </p>
          </div>
          <div>
            <span className="text-dark-400">Member Since</span>
            <p className="text-white mt-0.5">
              {new Date(user.createdAt).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            </p>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
