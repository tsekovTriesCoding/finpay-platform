import { Routes, Route } from 'react-router-dom';

import Layout from './components/layout/Layout';
import { ProtectedRoute } from './components/auth';
import { 
  HomePage, 
  LoginPage, 
  RegisterPage, 
  DashboardPage,
  OAuth2CallbackPage 
} from './pages';

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
      
      {/* Protected routes */}
      <Route 
        path="/dashboard" 
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        } 
      />
    </Routes>
  );
}

export default App;
