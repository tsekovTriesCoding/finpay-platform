import { Receipt } from 'lucide-react';

import type { TransactionDetail } from '../../../api/transactionDetailApi';
import { formatCurrency } from '../../dashboard/utils';
import { formatCategory } from './utils';

interface ReceiptSectionProps {
  detail: TransactionDetail;
  isSent: boolean;
}

export default function ReceiptSection({ detail, isSent }: ReceiptSectionProps) {
  const rows: { label: string; value: string }[] = [];

  // Type-specific rows
  if (detail.type === 'TRANSFER') {
    rows.push({ label: 'Type', value: isSent ? 'Money Sent' : 'Money Received' });
    if (detail.metadata.transferType === 'REQUEST_PAYMENT') {
      rows.push({ label: 'Triggered By', value: 'Money Request' });
    }
  } else if (detail.type === 'BILL_PAYMENT') {
    rows.push({ label: 'Biller', value: String(detail.metadata.billerName ?? '') });
    rows.push({ label: 'Category', value: formatCategory(String(detail.metadata.category ?? '')) });
    rows.push({ label: 'Account', value: String(detail.metadata.accountNumber ?? '') });
    if (detail.metadata.billerReference) {
      rows.push({ label: 'Biller Ref', value: String(detail.metadata.billerReference) });
    }
  } else if (detail.type === 'MONEY_REQUEST') {
    rows.push({ label: 'Type', value: 'Money Request' });
  }

  // Common rows
  rows.push({ label: 'Amount', value: formatCurrency(detail.amount) });
  if (detail.processingFee > 0) {
    rows.push({ label: 'Fee', value: formatCurrency(detail.processingFee) });
    rows.push({ label: 'Total', value: formatCurrency(detail.totalAmount) });
  }
  rows.push({ label: 'Currency', value: detail.currency });

  if (detail.description) {
    rows.push({ label: 'Note', value: detail.description });
  }

  rows.push({
    label: 'Date',
    value: new Date(detail.createdAt).toLocaleString('en-US', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }),
  });

  if (detail.completedAt) {
    rows.push({
      label: 'Completed',
      value: new Date(detail.completedAt).toLocaleString('en-US', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }),
    });
  }

  if (detail.failureReason) {
    rows.push({ label: 'Failure Reason', value: detail.failureReason });
  }

  return (
    <div className="bg-dark-800/60 rounded-2xl border border-dark-700/50 overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-dark-700/50">
        <Receipt className="w-4 h-4 text-dark-400" />
        <span className="text-sm font-semibold text-dark-300">Receipt Details</span>
      </div>
      <div className="divide-y divide-dark-700/30">
        {rows.map((row) => (
          <div
            key={row.label}
            className="flex items-center justify-between px-4 py-3"
          >
            <span className="text-sm text-dark-400">{row.label}</span>
            <span className="text-sm text-white font-medium text-right max-w-[60%] truncate">
              {row.value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
