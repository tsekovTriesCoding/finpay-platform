import { motion } from 'framer-motion';

/** Reusable skeleton loader for dashboard card sections. */
export default function SectionSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="card p-6 mb-8">
      <div className="flex items-center justify-between mb-4">
        <div className="h-5 w-40 bg-dark-800 rounded animate-pulse" />
        <div className="h-4 w-16 bg-dark-800 rounded animate-pulse" />
      </div>
      <div className="space-y-3">
        {Array.from({ length: rows }).map((_, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: i * 0.05 }}
            className="flex items-center justify-between p-3 bg-dark-800/50 rounded-lg border border-dark-700/50"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-dark-700 animate-pulse" />
              <div className="space-y-1.5">
                <div className="h-4 w-24 bg-dark-700 rounded animate-pulse" />
                <div className="h-3 w-36 bg-dark-700/60 rounded animate-pulse" />
              </div>
            </div>
            <div className="space-y-1.5 flex flex-col items-end">
              <div className="h-4 w-16 bg-dark-700 rounded animate-pulse" />
              <div className="h-5 w-20 bg-dark-700/60 rounded-full animate-pulse" />
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
