import { useQuery } from '@tanstack/react-query';

import { transactionDetailService } from '../api/transactionDetailApi';
import type { TransactionType } from '../api/transactionDetailApi';

export const transactionDetailKeys = {
  all: ['transactionDetail'] as const,
  transfer: (id: string) => [...transactionDetailKeys.all, 'transfer', id] as const,
  billPayment: (id: string) => [...transactionDetailKeys.all, 'billPayment', id] as const,
  moneyRequest: (id: string) => [...transactionDetailKeys.all, 'moneyRequest', id] as const,
};

/**
 * Fetch a unified transaction detail by type + id.
 * Automatically selects the right API endpoint based on transaction type.
 * Enables smart polling while transaction is in PROCESSING status.
 */
export function useTransactionDetail(
  type: TransactionType | null,
  id: string | null,
) {
  return useQuery({
    queryKey: type && id
      ? type === 'TRANSFER'
        ? transactionDetailKeys.transfer(id)
        : type === 'BILL_PAYMENT'
          ? transactionDetailKeys.billPayment(id)
          : transactionDetailKeys.moneyRequest(id)
      : transactionDetailKeys.all,

    queryFn: () => {
      if (!type || !id) throw new Error('Missing type or id');
      switch (type) {
        case 'TRANSFER':
          return transactionDetailService.getTransferDetail(id);
        case 'BILL_PAYMENT':
          return transactionDetailService.getBillPaymentDetail(id);
        case 'MONEY_REQUEST':
          return transactionDetailService.getMoneyRequestDetail(id);
      }
    },

    enabled: !!type && !!id,

    // Smart poll: refresh every 3s while transaction is still processing
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'PROCESSING' || status === 'PENDING' || status === 'COMPENSATING') {
        return 3_000;
      }
      return false;
    },

    staleTime: 10_000,
  });
}
