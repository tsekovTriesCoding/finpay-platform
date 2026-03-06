import { Shield, ShieldCheck, Store } from 'lucide-react';

/* User status badge colours */
export const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-green-500/20 text-green-400',
  INACTIVE: 'bg-gray-500/20 text-gray-400',
  SUSPENDED: 'bg-red-500/20 text-red-400',
  PENDING_VERIFICATION: 'bg-yellow-500/20 text-yellow-400',
};

/* Role badge colours */
export const ROLE_COLORS: Record<string, string> = {
  USER: 'bg-blue-500/20 text-blue-400',
  ADMIN: 'bg-purple-500/20 text-purple-400',
  MERCHANT: 'bg-emerald-500/20 text-emerald-400',
};

/* Role → icon mapping */
export const ROLE_ICONS: Record<string, typeof Shield> = {
  USER: Shield,
  ADMIN: ShieldCheck,
  MERCHANT: Store,
};

/* KPI card colour variants */
export const KPI_COLOR_MAP = {
  blue: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  green: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  red: 'bg-red-500/10 text-red-400 border-red-500/20',
  yellow: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20',
  purple: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
} as const;

export type KPIColor = keyof typeof KPI_COLOR_MAP;
