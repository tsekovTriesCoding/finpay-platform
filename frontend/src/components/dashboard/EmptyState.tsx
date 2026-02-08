import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
}

/** Shown when a section has loaded successfully but contains no data. */
export default function EmptyState({ icon, title, description }: EmptyStateProps) {
  return (
    <div className="card p-6 mb-8">
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <div className="w-12 h-12 bg-dark-800 rounded-full flex items-center justify-center mb-3">
          {icon ?? <Inbox className="w-6 h-6 text-dark-500" />}
        </div>
        <p className="text-sm font-medium text-dark-300">{title}</p>
        {description && (
          <p className="text-xs text-dark-500 mt-1 max-w-xs">{description}</p>
        )}
      </div>
    </div>
  );
}
