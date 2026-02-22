import type { LucideIcon } from 'lucide-react';
import { Zap, Sparkles, Building2 } from 'lucide-react';

import type { AccountPlan } from '../../api';

/* Types */

export interface PlanConfig {
  id: AccountPlan;
  name: string;
  icon: LucideIcon;
  price: string;
  period: string;
  description: string;
  features: readonly string[];
  limits: { daily: string; monthly: string };
  gradient: string;
  popular: boolean;
}

export interface RegisterFormState {
  error: string | null;
}

/* Constants */

export const VALID_PLANS: readonly AccountPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];

export const PLANS: readonly PlanConfig[] = [
  {
    id: 'STARTER',
    name: 'Starter',
    icon: Zap,
    price: '0',
    period: 'Free forever',
    description: 'Perfect for individuals getting started with digital payments.',
    features: [
      'Up to 10 transactions/month',
      '1 virtual card',
      'Basic analytics dashboard',
      'Email support',
      'Mobile app access',
    ],
    limits: { daily: '$500', monthly: '$5,000' },
    gradient: 'from-dark-700 to-dark-800',
    popular: false,
  },
  {
    id: 'PRO',
    name: 'Pro',
    icon: Sparkles,
    price: '29',
    period: '/month',
    description: 'For growing businesses that need more power and flexibility.',
    features: [
      'Unlimited transactions',
      '10 virtual cards',
      'Advanced analytics & reports',
      'Priority email & chat support',
      'Multi-currency accounts',
      'Recurring payments',
      'API access',
    ],
    limits: { daily: '$10,000', monthly: '$100,000' },
    gradient: 'from-primary-600 to-primary-500',
    popular: true,
  },
  {
    id: 'ENTERPRISE',
    name: 'Enterprise',
    icon: Building2,
    price: 'Custom',
    period: 'Contact us',
    description: 'Tailored solutions for large organizations with complex needs.',
    features: [
      'Everything in Pro',
      'Unlimited virtual & physical cards',
      'Custom integrations',
      'Dedicated account manager',
      'SLA guarantee (99.99%)',
      'Advanced fraud protection',
      'Custom compliance reports',
    ],
    limits: { daily: '$1,000,000', monthly: '$10,000,000' },
    gradient: 'from-secondary-600 to-secondary-500',
    popular: false,
  },
];

export const PASSWORD_REQUIREMENTS = [
  { label: 'At least 8 characters', test: (pw: string) => pw.length >= 8 },
  { label: 'Contains a number', test: (pw: string) => /\d/.test(pw) },
  { label: 'Contains uppercase letter', test: (pw: string) => /[A-Z]/.test(pw) },
  { label: 'Contains lowercase letter', test: (pw: string) => /[a-z]/.test(pw) },
] as const;
