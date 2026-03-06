import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  ArrowLeftRight,
  Wallet,
  ScrollText,
  Bell,
  Activity,
  LogOut,
  Shield,
  ChevronLeft,
} from 'lucide-react';

import { useAuth } from '../../contexts/AuthContext';

const navItems = [
  { to: '/admin', icon: LayoutDashboard, label: 'Dashboard', end: true },
  { to: '/admin/users', icon: Users, label: 'User Management' },
  { to: '/admin/transactions', icon: ArrowLeftRight, label: 'Transactions' },
  { to: '/admin/wallets', icon: Wallet, label: 'Wallets' },
  { to: '/admin/audit-logs', icon: ScrollText, label: 'Audit Logs' },
  { to: '/admin/system', icon: Activity, label: 'System Overview' },
  { to: '/admin/notifications', icon: Bell, label: 'Notifications' },
];

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-dark-950 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-dark-900 border-r border-dark-700 flex flex-col fixed h-full">
        {/* Logo area */}
        <div className="px-6 py-5 border-b border-dark-700">
          <div className="flex items-center gap-2">
            <Shield className="w-7 h-7 text-primary-500" />
            <span className="text-lg font-bold text-white">FinPay Admin</span>
          </div>
          <p className="text-xs text-dark-500 mt-1">{user?.email}</p>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, icon: Icon, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-600/20 text-primary-400 border-l-2 border-primary-500'
                    : 'text-dark-400 hover:text-dark-200 hover:bg-dark-800'
                }`
              }
            >
              <Icon className="w-5 h-5 flex-shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Footer actions */}
        <div className="px-3 py-4 border-t border-dark-700 space-y-1">
          <button
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium
                       text-dark-400 hover:text-dark-200 hover:bg-dark-800 transition-colors w-full"
          >
            <ChevronLeft className="w-5 h-5" />
            Back to App
          </button>
          <button
            onClick={logout}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium
                       text-red-400 hover:text-red-300 hover:bg-dark-800 transition-colors w-full"
          >
            <LogOut className="w-5 h-5" />
            Logout
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 ml-64 p-8">
        <Outlet />
      </main>
    </div>
  );
}
