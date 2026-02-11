import type { TransactionDetail } from '../../../api/transactionDetailApi';
import { formatCurrency } from '../../dashboard/utils';
import { getStatusConfig, formatStatus } from './utils';

interface TransactionHeaderProps {
  detail: TransactionDetail;
  isSent: boolean;
}

export default function TransactionHeader({ detail, isSent }: TransactionHeaderProps) {
  const statusConfig = getStatusConfig(detail.status);

  return (
    <div className="text-center pt-2">
      <div
        className={`w-16 h-16 mx-auto rounded-full flex items-center justify-center mb-3 ${statusConfig.bgClass}`}
      >
        <statusConfig.icon className={`w-8 h-8 ${statusConfig.textClass}`} />
      </div>

      <p className={`text-3xl font-bold ${isSent ? 'text-red-400' : 'text-secondary-400'}`}>
        {isSent ? '−' : '+'}
        {formatCurrency(detail.totalAmount)}
      </p>

      <p className="text-white font-medium mt-1">{detail.title}</p>

      <span
        className={`inline-flex items-center gap-1.5 mt-2 px-3 py-1 rounded-full text-xs font-semibold ${statusConfig.badgeClass}`}
      >
        <statusConfig.icon className="w-3 h-3" />
        {formatStatus(detail.status)}
      </span>
    </div>
  );
}
