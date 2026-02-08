import {
  Zap,
  Droplets,
  Wifi,
  Smartphone,
  Flame,
  Shield,
  Home,
  Tv,
  Cloud,
  Building2,
  GraduationCap,
  FileText,
  type LucideIcon,
} from 'lucide-react';

import type { BillCategory } from '../../../api/billPaymentApi';

/** Map icon name strings from the biller catalogue to Lucide components */
export const ICON_MAP: Record<string, LucideIcon> = {
  Zap,
  Droplets,
  Wifi,
  Smartphone,
  Flame,
  Shield,
  Home,
  Tv,
  Cloud,
  Building2,
  GraduationCap,
  FileText,
};

export const CATEGORY_ICONS: Record<BillCategory, LucideIcon> = {
  ELECTRICITY: Zap,
  WATER: Droplets,
  INTERNET: Wifi,
  PHONE: Smartphone,
  GAS: Flame,
  INSURANCE: Shield,
  RENT: Home,
  SUBSCRIPTION: Tv,
  GOVERNMENT: Building2,
  EDUCATION: GraduationCap,
  OTHER: FileText,
};

export const CATEGORY_COLORS: Record<BillCategory, string> = {
  ELECTRICITY: 'bg-yellow-500',
  WATER: 'bg-blue-500',
  INTERNET: 'bg-cyan-500',
  PHONE: 'bg-green-500',
  GAS: 'bg-orange-500',
  INSURANCE: 'bg-indigo-500',
  RENT: 'bg-pink-500',
  SUBSCRIPTION: 'bg-purple-500',
  GOVERNMENT: 'bg-slate-500',
  EDUCATION: 'bg-teal-500',
  OTHER: 'bg-gray-500',
};

export type PayBillStep =
  | 'category'
  | 'biller'
  | 'form'
  | 'confirming'
  | 'processing'
  | 'success'
  | 'error';

export const getIcon = (iconName: string): LucideIcon => ICON_MAP[iconName] ?? FileText;

export const formatCurrency = (value: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
