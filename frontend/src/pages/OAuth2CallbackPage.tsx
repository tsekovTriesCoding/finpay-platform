import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Loader2, CheckCircle, XCircle } from 'lucide-react';

import { authService } from '../api';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState<string>('');

  useEffect(() => {
    const handleCallback = async () => {
      const error = searchParams.get('error');

      if (error) {
        setStatus('error');
        setErrorMessage(error);
        return;
      }

      try {
        // With HTTP-only cookies, tokens are already set by the backend redirect
        // Just fetch user data to verify the session and store user info locally
        const user = await authService.getCurrentUser();
        localStorage.setItem('user', JSON.stringify(user));

        setStatus('success');
        
        // Navigate to dashboard after a brief delay
        setTimeout(() => {
          navigate('/dashboard', { replace: true });
        }, 1500);
      } catch {
        setStatus('error');
        setErrorMessage('Failed to complete authentication');
      }
    };

    handleCallback();
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.3 }}
        className="bg-white rounded-2xl shadow-xl p-12 text-center max-w-md mx-4"
      >
        {status === 'loading' && (
          <>
            <Loader2 className="w-16 h-16 text-blue-600 animate-spin mx-auto mb-6" />
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Completing Sign In</h2>
            <p className="text-gray-500">Please wait while we verify your account...</p>
          </>
        )}

        {status === 'success' && (
          <>
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', stiffness: 200, damping: 15 }}
            >
              <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-6" />
            </motion.div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Sign In Successful!</h2>
            <p className="text-gray-500">Redirecting you to your dashboard...</p>
          </>
        )}

        {status === 'error' && (
          <>
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', stiffness: 200, damping: 15 }}
            >
              <XCircle className="w-16 h-16 text-red-500 mx-auto mb-6" />
            </motion.div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Sign In Failed</h2>
            <p className="text-gray-500 mb-6">{errorMessage}</p>
            <button
              onClick={() => navigate('/login')}
              className="px-6 py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium rounded-lg hover:from-blue-700 hover:to-purple-700 transition-all"
            >
              Return to Login
            </button>
          </>
        )}
      </motion.div>
    </div>
  );
}
