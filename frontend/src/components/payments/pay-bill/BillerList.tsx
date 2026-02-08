import { Search, ChevronRight } from 'lucide-react';

import type { Biller } from '../../../api/billPaymentApi';
import { CATEGORY_COLORS, getIcon } from './constants';

interface BillerListProps {
  billers: Biller[];
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onSelect: (biller: Biller) => void;
}

export default function BillerList({
  billers,
  searchQuery,
  onSearchChange,
  onSelect,
}: BillerListProps) {
  return (
    <div className="space-y-4">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-400" />
        <input
          type="text"
          placeholder="Search billers..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 bg-dark-800 border border-dark-700 rounded-lg text-white placeholder-dark-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 text-sm"
        />
      </div>

      {billers.length === 0 ? (
        <p className="text-dark-400 text-center py-8 text-sm">No billers found</p>
      ) : (
        <div className="space-y-2">
          {billers.map((biller) => {
            const Icon = getIcon(biller.icon);
            const color = CATEGORY_COLORS[biller.category];
            return (
              <button
                key={biller.code}
                onClick={() => onSelect(biller)}
                className="w-full flex items-center gap-3 p-3 rounded-xl bg-dark-800/50 border border-dark-700/50 hover:border-purple-500/50 hover:bg-dark-800 transition-all group"
              >
                <div
                  className={`w-10 h-10 ${color} rounded-full flex items-center justify-center shrink-0`}
                >
                  <Icon className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1 text-left">
                  <p className="text-sm font-medium text-white">{biller.name}</p>
                  <p className="text-xs text-dark-400">{biller.code}</p>
                </div>
                <ChevronRight className="w-4 h-4 text-dark-400 group-hover:text-white transition-colors" />
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
