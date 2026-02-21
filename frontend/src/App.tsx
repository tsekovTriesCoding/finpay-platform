import { createBrowserRouter, Outlet } from 'react-router-dom';

import Layout from './components/layout/Layout';
import { ProtectedRoute } from './components/auth';
import { useAuth } from './contexts/AuthContext';
import { AuthProvider } from './contexts';
import { NotificationProvider } from './components/notifications';

type PageModule = { default: React.ComponentType };
const page = (importFn: () => Promise<PageModule>) =>
  () => importFn().then((m) => ({ Component: m.default }));

function RootLayout() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  );
}

function AuthenticatedLayout() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <NotificationProvider userId={user.id}>
      <Outlet />
    </NotificationProvider>
  );
}

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      {
        element: <Layout />,
        children: [
          { index: true, lazy: page(() => import('./pages/HomePage')) },
          { path: 'pricing', lazy: page(() => import('./pages/PricingPage')) },
          { path: 'about', lazy: page(() => import('./pages/AboutPage')) },
          { path: 'contact', lazy: page(() => import('./pages/ContactPage')) },
          { path: 'demo', lazy: page(() => import('./pages/DemoPage')) },
          { path: 'docs', lazy: page(() => import('./pages/DocsPage')) },
          { path: 'blog', lazy: page(() => import('./pages/BlogPage')) },
          { path: 'careers', lazy: page(() => import('./pages/CareersPage')) },
          { path: 'privacy', lazy: page(() => import('./pages/PrivacyPage')) },
          { path: 'terms', lazy: page(() => import('./pages/TermsPage')) },
          { path: 'cookies', lazy: page(() => import('./pages/CookiePolicyPage')) },
          { path: 'compliance', lazy: page(() => import('./pages/CompliancePage')) },
          { path: 'features/:slug', lazy: page(() => import('./pages/FeatureDetailPage')) },
        ],
      },

      { path: 'login', lazy: page(() => import('./pages/LoginPage')) },
      { path: 'register', lazy: page(() => import('./pages/RegisterPage')) },
      { path: 'oauth2/callback', lazy: page(() => import('./pages/OAuth2CallbackPage')) },

      {
        element: (
          <ProtectedRoute>
            <AuthenticatedLayout />
          </ProtectedRoute>
        ),
        children: [
          { path: 'dashboard', lazy: page(() => import('./pages/DashboardPage')) },
          { path: 'settings', lazy: page(() => import('./pages/SettingsPage')) },
        ],
      },
    ],
  },
]);

export default router;
