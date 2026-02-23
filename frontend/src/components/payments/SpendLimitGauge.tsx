import { TrendingUp } from 'lucide-react';

interface SpendLimitGaugeProps {
  label: string;
  remaining: number;
  limit: number;
  suffix: string;
  formatCurrency: (value: number) => string;
}

/**
 * Compact gauge showing remaining spend allowance with a color-coded
 * progress bar.  Turns amber at 25 % remaining and red at 10 %.
 */
export default function SpendLimitGauge({
  label,
  remaining,
  limit,
  suffix,
  formatCurrency,
}: SpendLimitGaugeProps) {
  const ratio = limit > 0 ? remaining / limit : 0;
  const color =
    ratio <= 0.1 ? 'red' : ratio <= 0.25 ? 'amber' : 'secondary';

  const textColor = {
    red: 'text-red-400',
    amber: 'text-amber-400',
    secondary: 'text-secondary-400',
  }[color];

  const barColor = {
    red: 'bg-red-500',
    amber: 'bg-amber-500',
    secondary: 'bg-secondary-500',
  }[color];

  return (
    <div>
      <div className="flex items-center gap-1.5 mb-1">
        <TrendingUp className="w-3.5 h-3.5 text-dark-500" />
        <span className="text-xs text-dark-500 uppercase tracking-wide">
          {label}
        </span>
      </div>

      <p className={`text-sm font-semibold ${textColor}`}>
        {formatCurrency(remaining)}
      </p>

      <div className="mt-1 h-1 bg-dark-700 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${barColor}`}
          style={{ width: `${ratio * 100}%` }}
        />
      </div>

      <p className="text-[10px] text-dark-500 mt-0.5">
        of {formatCurrency(limit)}/{suffix}
      </p>
    </div>
  );
}
