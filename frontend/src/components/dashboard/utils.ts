/** Shared helpers for dashboard components. */
export function formatCurrency(amount: number) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

/** Map a transfer / bill status to Tailwind badge classes. */
export function statusBadgeClasses(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return 'bg-secondary-500/20 text-secondary-400';
    case 'FAILED':
    case 'COMPENSATED':
    case 'REFUNDED':
      return 'bg-red-500/20 text-red-400';
    case 'CANCELLED':
      return 'bg-dark-700 text-dark-300';
    default:
      // PENDING, PROCESSING, COMPENSATING
      return 'bg-yellow-500/20 text-yellow-400';
  }
}
