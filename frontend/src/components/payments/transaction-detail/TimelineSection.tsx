import {
  Clock,
  CheckCircle2,
  XCircle,
  Loader2,
  ChevronRight,
  type LucideIcon,
} from 'lucide-react';

import type { StatusTimelineEntry } from '../../../api/transactionDetailApi';

// Timeline Container

interface TimelineSectionProps {
  timeline: StatusTimelineEntry[];
}

export default function TimelineSection({ timeline }: TimelineSectionProps) {
  return (
    <div className="bg-dark-800/60 rounded-2xl border border-dark-700/50 overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-dark-700/50">
        <Clock className="w-4 h-4 text-dark-400" />
        <span className="text-sm font-semibold text-dark-300">Status Timeline</span>
      </div>

      <div className="px-4 py-3">
        <div className="relative">
          {timeline.map((entry, idx) => (
            <TimelineStep
              key={entry.status + idx}
              entry={entry}
              isLast={idx === timeline.length - 1}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

// Individual Step

function TimelineStep({ entry, isLast }: { entry: StatusTimelineEntry; isLast: boolean }) {
  let dotClass: string;
  let lineClass: string;
  let Icon: LucideIcon;

  if (entry.completed) {
    dotClass = 'bg-secondary-500 ring-secondary-500/30';
    lineClass = 'bg-secondary-500/40';
    Icon = CheckCircle2;
  } else if (entry.current) {
    dotClass = 'bg-primary-500 ring-primary-500/30 animate-pulse';
    lineClass = 'bg-dark-600';
    Icon = Loader2;
  } else if (entry.failed) {
    dotClass = 'bg-red-500 ring-red-500/30';
    lineClass = 'bg-red-500/40';
    Icon = XCircle;
  } else {
    dotClass = 'bg-dark-600 ring-dark-500/30';
    lineClass = 'bg-dark-700';
    Icon = ChevronRight;
  }

  return (
    <div className="flex gap-3 pb-4 last:pb-0">
      <div className="flex flex-col items-center">
        <div
          className={`w-7 h-7 rounded-full ring-4 flex items-center justify-center shrink-0 ${dotClass}`}
        >
          <Icon
            className={`w-3.5 h-3.5 text-white ${entry.current ? 'animate-spin' : ''}`}
          />
        </div>
        {!isLast && <div className={`w-0.5 flex-1 mt-1 rounded-full ${lineClass}`} />}
      </div>

      <div className="pt-0.5 min-w-0 flex-1">
        <p
          className={`text-sm font-semibold ${
            entry.completed || entry.current
              ? 'text-white'
              : entry.failed
                ? 'text-red-400'
                : 'text-dark-500'
          }`}
        >
          {entry.label}
        </p>
        <p className={`text-xs mt-0.5 ${entry.failed ? 'text-red-400/80' : 'text-dark-400'}`}>
          {entry.description}
        </p>
        {entry.timestamp && (
          <p className="text-xs text-dark-500 mt-0.5">
            {new Date(entry.timestamp).toLocaleString('en-US', {
              dateStyle: 'short',
              timeStyle: 'short',
            })}
          </p>
        )}
      </div>
    </div>
  );
}
