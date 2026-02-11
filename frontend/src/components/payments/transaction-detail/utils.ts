import {
  Clock,
  CheckCircle2,
  XCircle,
  Loader2,
  type LucideIcon,
} from 'lucide-react';

export interface StatusConfig {
  icon: LucideIcon;
  bgClass: string;
  textClass: string;
  badgeClass: string;
}

export function getStatusConfig(status: string): StatusConfig {
  switch (status) {
    case 'COMPLETED':
      return {
        icon: CheckCircle2,
        bgClass: 'bg-secondary-500/20',
        textClass: 'text-secondary-400',
        badgeClass: 'bg-secondary-500/20 text-secondary-400',
      };
    case 'FAILED':
    case 'COMPENSATED':
    case 'CANCELLED':
      return {
        icon: XCircle,
        bgClass: 'bg-red-500/20',
        textClass: 'text-red-400',
        badgeClass: 'bg-red-500/20 text-red-400',
      };
    case 'PENDING':
    case 'PENDING_APPROVAL':
    case 'PROCESSING':
    case 'COMPENSATING':
      return {
        icon: Loader2,
        bgClass: 'bg-yellow-500/20',
        textClass: 'text-yellow-400',
        badgeClass: 'bg-yellow-500/20 text-yellow-400',
      };
    default:
      return {
        icon: Clock,
        bgClass: 'bg-dark-700',
        textClass: 'text-dark-400',
        badgeClass: 'bg-dark-700 text-dark-300',
      };
  }
}

export function formatStatus(status: string): string {
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function formatCategory(category: string): string {
  return category
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
