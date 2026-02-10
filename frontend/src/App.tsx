import { Routes, Route, Outlet } from 'react-router-dom';

import Layout from './components/layout/Layout';
import { ProtectedRoute } from './components/auth';
import { useAuth } from './contexts/AuthContext';
import { NotificationProvider } from './components/notifications';
import { 
  HomePage, 
  LoginPage, 
  RegisterPage, 
  DashboardPage,
  OAuth2CallbackPage,
  SettingsPage 
} from './pages';

/**
 * Wraps all authenticated routes with the notification WebSocket provider.
 * This keeps the real-time connection alive across page navigations
 * (dashboard, settings, etc.) without reconnecting on each route change.
 */
function AuthenticatedLayout() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <NotificationProvider userId={user.id}>
      <Outlet />
    </NotificationProvider>
  );
}

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<Layout />}>
        <Route index element={<HomePage />} />
      </Route>
      
      {/* Auth routes (no layout) */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
      
      {/* Protected routes — share a single WebSocket connection */}
      <Route element={<ProtectedRoute><AuthenticatedLayout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  );
}

export default App;
