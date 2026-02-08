import { useMemo } from 'react';

import {
  BILL_CATEGORY_LABELS,
  type BillCategory,
} from '../../../api/billPaymentApi';
import { CATEGORY_ICONS, CATEGORY_COLORS } from './constants';

interface CategoryGridProps {
  onSelect: (category: BillCategory) => void;
}

export default function CategoryGrid({ onSelect }: CategoryGridProps) {
  const categories = useMemo(
    () => Object.keys(BILL_CATEGORY_LABELS) as BillCategory[],
    [],
  );

  return (
    <div className="grid grid-cols-3 gap-3">
      {categories.map((cat) => {
        const Icon = CATEGORY_ICONS[cat];
        const color = CATEGORY_COLORS[cat];
        return (
          <button
            key={cat}
            onClick={() => onSelect(cat)}
            className="flex flex-col items-center gap-2 p-4 rounded-xl bg-dark-800/50 border border-dark-700/50 hover:border-purple-500/50 hover:bg-dark-800 transition-all group"
          >
            <div
              className={`w-12 h-12 ${color} rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform`}
            >
              <Icon className="w-6 h-6 text-white" />
            </div>
            <span className="text-xs font-medium text-dark-300 group-hover:text-white transition-colors text-center">
              {BILL_CATEGORY_LABELS[cat]}
            </span>
          </button>
        );
      })}
    </div>
  );
}
