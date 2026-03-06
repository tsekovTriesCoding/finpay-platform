import { motion } from 'framer-motion';

import { KPI_COLOR_MAP, type KPIColor } from './constants';

export default function KPICard({
  title,
  value,
  icon: Icon,
  color,
  loading,
}: {
  title: string;
  value: number | string | undefined;
  icon: React.ComponentType<{ className?: string }>;
  color: KPIColor;
  loading: boolean;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`rounded-xl border p-5 ${KPI_COLOR_MAP[color]}`}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider opacity-70">{title}</p>
          {loading ? (
            <div className="h-8 w-20 bg-dark-700 rounded animate-pulse mt-1" />
          ) : (
            <p className="text-2xl font-bold mt-1">{value ?? '—'}</p>
          )}
        </div>
        <Icon className="w-8 h-8 opacity-50" />
      </div>
    </motion.div>
  );
}
