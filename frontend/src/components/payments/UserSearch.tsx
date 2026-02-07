import { useState, useEffect, useRef } from 'react';
import { Search, User, X, Loader2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

import { UserSearchResult } from '../../api';
import { useUserSearch } from '../../hooks';

interface UserSearchProps {
  excludeUserId: string;
  onUserSelect: (user: UserSearchResult) => void;
  selectedUser: UserSearchResult | null;
  onClear: () => void;
}

export default function UserSearch({ 
  excludeUserId, 
  onUserSelect, 
  selectedUser,
  onClear 
}: UserSearchProps) {
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const { query, debouncedQuery, results, isLoading, error, search, clear } = useUserSearch({
    excludeUserId,
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    search(e.target.value);
    setShowDropdown(true);
  };

  const handleUserSelect = (user: UserSearchResult) => {
    onUserSelect(user);
    clear();
    setShowDropdown(false);
  };

  const handleClear = () => {
    onClear();
    clear();
    inputRef.current?.focus();
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName?.[0] || ''}${lastName?.[0] || ''}`.toUpperCase();
  };

  if (selectedUser) {
    return (
      <div className="bg-primary-500/10 rounded-xl p-4 border border-primary-500/30">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {selectedUser.profileImageUrl ? (
              <img
                src={selectedUser.profileImageUrl}
                alt={`${selectedUser.firstName} ${selectedUser.lastName}`}
                className="w-12 h-12 rounded-full object-cover"
              />
            ) : (
              <div className="w-12 h-12 bg-gradient-to-br from-primary-600 to-primary-500 rounded-full flex items-center justify-center text-white font-medium shadow-lg shadow-primary-500/25">
                {getInitials(selectedUser.firstName, selectedUser.lastName)}
              </div>
            )}
            <div>
              <p className="font-medium text-white">
                {selectedUser.firstName} {selectedUser.lastName}
              </p>
              <p className="text-sm text-dark-400">{selectedUser.email}</p>
            </div>
          </div>
          <button
            onClick={handleClear}
            className="p-2 text-dark-400 hover:text-white hover:bg-dark-700/50 rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          {isLoading ? (
            <Loader2 className="h-5 w-5 text-dark-500 animate-spin" />
          ) : (
            <Search className="h-5 w-5 text-dark-500" />
          )}
        </div>
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={() => results.length > 0 && setShowDropdown(true)}
          placeholder="Search by name or email..."
          className="block w-full pl-10 pr-4 py-3 bg-dark-800/50 border border-dark-700/50 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all text-white placeholder-dark-500"
        />
      </div>

      {error && (
        <p className="mt-2 text-sm text-red-400">{error}</p>
      )}

      <AnimatePresence>
        {showDropdown && results.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
            className="absolute z-50 w-full mt-2 bg-dark-800 rounded-xl shadow-lg border border-dark-700/50 overflow-hidden"
          >
            <ul className="max-h-64 overflow-y-auto">
              {results.map((user) => (
                <li key={user.id}>
                  <button
                    onClick={() => handleUserSelect(user)}
                    className="w-full px-4 py-3 flex items-center gap-3 hover:bg-dark-700/50 transition-colors text-left"
                  >
                    {user.profileImageUrl ? (
                      <img
                        src={user.profileImageUrl}
                        alt={`${user.firstName} ${user.lastName}`}
                        className="w-10 h-10 rounded-full object-cover"
                      />
                    ) : (
                      <div className="w-10 h-10 bg-gradient-to-br from-primary-600 to-primary-500 rounded-full flex items-center justify-center text-white text-sm font-medium shadow-lg shadow-primary-500/25">
                        {getInitials(user.firstName, user.lastName)}
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-white truncate">
                        {user.firstName} {user.lastName}
                      </p>
                      <p className="text-sm text-dark-400 truncate">{user.email}</p>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          </motion.div>
        )}
      </AnimatePresence>

      {debouncedQuery.length >= 2 && !isLoading && results.length === 0 && showDropdown && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="absolute z-50 w-full mt-2 bg-dark-800 rounded-xl shadow-lg border border-dark-700/50 p-4"
        >
          <div className="flex items-center gap-3 text-dark-400">
            <User className="w-5 h-5" />
            <p>No users found matching &quot;{debouncedQuery}&quot;</p>
          </div>
        </motion.div>
      )}
    </div>
  );
}
