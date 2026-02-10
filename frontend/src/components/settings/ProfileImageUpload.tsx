import { useState, useRef, useCallback } from 'react';
import { Camera, Trash2, Loader2 } from 'lucide-react';
import { motion } from 'framer-motion';

import type { User } from '../../api/authApi';
import { userService } from '../../api/userApi';

interface ProfileImageUploadProps {
  user: User;
  onImageUpdated: (url: string | null) => void;
}

export default function ProfileImageUpload({ user, onImageUpdated }: ProfileImageUploadProps) {
  const [isUploading, setIsUploading] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (!file) return;

      if (!file.type.startsWith('image/')) {
        setError('Please select an image file');
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        setError('Image must be less than 5MB');
        return;
      }

      setError(null);
      setIsUploading(true);

      try {
        const result = await userService.uploadProfileImage(user.id, file);
        onImageUpdated(result.profileImageUrl);
      } catch {
        setError('Failed to upload image. Please try again.');
      } finally {
        setIsUploading(false);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      }
    },
    [user.id, onImageUpdated],
  );

  const handleDelete = useCallback(async () => {
    setIsDeleting(true);
    setError(null);

    try {
      await userService.deleteProfileImage(user.id);
      onImageUpdated(null);
    } catch {
      setError('Failed to remove image. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  }, [user.id, onImageUpdated]);

  const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`;

  return (
    <div className="flex flex-col items-center gap-4">
      <div className="relative group">
        <div className="w-28 h-28 rounded-full overflow-hidden border-4 border-dark-700 shadow-lg shadow-primary-500/10">
          {user.profileImageUrl ? (
            <img
              src={user.profileImageUrl}
              alt={`${user.firstName} ${user.lastName}`}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-primary-600 to-primary-500 flex items-center justify-center text-white text-3xl font-bold">
              {initials}
            </div>
          )}

          {(isUploading || isDeleting) && (
            <div className="absolute inset-0 rounded-full bg-black/60 flex items-center justify-center">
              <Loader2 className="w-8 h-8 text-white animate-spin" />
            </div>
          )}
        </div>

        {!isUploading && !isDeleting && (
          <motion.button
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => fileInputRef.current?.click()}
            className="absolute bottom-0 right-0 w-10 h-10 bg-primary-600 hover:bg-primary-500 rounded-full flex items-center justify-center text-white shadow-lg transition-colors border-2 border-dark-800"
            title="Upload photo"
          >
            <Camera className="w-5 h-5" />
          </motion.button>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileSelect}
        className="hidden"
      />

      {user.profileImageUrl && !isUploading && !isDeleting && (
        <button
          onClick={handleDelete}
          className="flex items-center gap-2 text-sm text-dark-400 hover:text-red-400 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
          Remove photo
        </button>
      )}

      {error && (
        <p className="text-sm text-red-400">{error}</p>
      )}

      <p className="text-xs text-dark-500">
        JPG, PNG or GIF. Max 5MB.
      </p>
    </div>
  );
}
