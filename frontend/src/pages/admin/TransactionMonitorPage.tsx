import { useState, useMemo } from 'react';
import { type ColumnDef, type SortingState, type PaginationState } from '@tanstack/react-table';
import { motion } from 'framer-motion';
import { ArrowLeftRight, Receipt, HandCoins } from 'lucide-react';

import DataTable from '../../components/admin/DataTable';
import { useAdminTransactions } from '../../hooks/useAdmin';
import type { AdminTransaction } from '../../api/adminApi';
import { formatDate, formatCurrency } from '../../utils/exportUtils';

const TYPE_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  TRANSFER: ArrowLeftRight,
  BILL_PAYMENT: Receipt,
  MONEY_REQUEST: HandCoins,
};

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: 'bg-green-500/20 text-green-400',
  PENDING: 'bg-yellow-500/20 text-yellow-400',
  PENDING_APPROVAL: 'bg-yellow-500/20 text-yellow-400',
  PROCESSING: 'bg-blue-500/20 text-blue-400',
  FAILED: 'bg-red-500/20 text-red-400',
  CANCELLED: 'bg-gray-500/20 text-gray-400',
  DECLINED: 'bg-red-500/20 text-red-300',
  COMPENSATED: 'bg-orange-500/20 text-orange-400',
  COMPENSATING: 'bg-orange-500/20 text-orange-300',
};

export default function TransactionMonitorPage() {
  const [typeFilter, setTypeFilter] = useState('TRANSFER');
  const [statusFilter, setStatusFilter] = useState('');
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [sorting, setSorting] = useState<SortingState>([{ id: 'createdAt', desc: true }]);

  const sortBy = sorting[0]?.id || 'createdAt';
  const sortDir = sorting[0]?.desc ? 'desc' : 'asc';

  const { data, isLoading } = useAdminTransactions({
    type: typeFilter || undefined,
    status: statusFilter || undefined,
    sortBy,
    sortDir: sortDir as 'asc' | 'desc',
    page: pagination.pageIndex,
    size: pagination.pageSize,
  });

  const columns = useMemo<ColumnDef<AdminTransaction, unknown>[]>(
    () => [
      {
        accessorKey: 'type',
        header: 'Type',
        enableSorting: false,
        cell: ({ getValue }) => {
          const type = getValue() as string;
          const Icon = TYPE_ICONS[type] || ArrowLeftRight;
          return (
            <div className="flex items-center gap-2">
              <Icon className="w-4 h-4 text-gray-400" />
              <span className="text-xs font-medium text-gray-300">
                {type.replace('_', ' ')}
              </span>
            </div>
          );
        },
      },
      {
        accessorKey: 'transactionReference',
        header: 'Reference',
        cell: ({ getValue }) => (
          <code className="text-xs text-gray-400 font-mono">
            {(getValue() as string).substring(0, 12)}...
          </code>
        ),
      },
      {
        accessorKey: 'amount',
        header: 'Amount',
        cell: ({ row }) => (
          <span className="font-medium text-white">
            {formatCurrency(row.original.amount, row.original.currency)}
          </span>
        ),
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ getValue }) => {
          const status = getValue() as string;
          return (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[status] || 'bg-gray-500/20 text-gray-400'}`}>
              {status.replace('_', ' ')}
            </span>
          );
        },
      },
      {
        accessorKey: 'fromUserId',
        header: 'From',
        cell: ({ getValue }) => {
          const id = getValue() as string | null;
          return id ? (
            <code className="text-xs text-gray-400 font-mono">{id.substring(0, 8)}...</code>
          ) : <span className="text-gray-600">—</span>;
        },
      },
      {
        accessorKey: 'toUserId',
        header: 'To',
        cell: ({ getValue }) => {
          const id = getValue() as string | null;
          return id ? (
            <code className="text-xs text-gray-400 font-mono">{id.substring(0, 8)}...</code>
          ) : <span className="text-gray-600">—</span>;
        },
      },
      {
        accessorKey: 'description',
        header: 'Description',
        enableSorting: false,
        cell: ({ getValue }) => (
          <span className="text-gray-400 max-w-[200px] truncate block">
            {(getValue() as string | null) || '—'}
          </span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Date',
        cell: ({ getValue }) => (
          <span className="text-gray-400 text-xs">{formatDate(getValue() as string)}</span>
        ),
      },
    ],
    []
  );

  const exportColumns = [
    { header: 'Type', accessor: 'type' as const },
    { header: 'Reference', accessor: 'transactionReference' as const },
    { header: 'Amount', accessor: ((row: AdminTransaction) => formatCurrency(row.amount, row.currency)) },
    { header: 'Status', accessor: 'status' as const },
    { header: 'From', accessor: ((row: AdminTransaction) => row.fromUserId || '') },
    { header: 'To', accessor: ((row: AdminTransaction) => row.toUserId || '') },
    { header: 'Description', accessor: ((row: AdminTransaction) => row.description || '') },
    { header: 'Date', accessor: ((row: AdminTransaction) => formatDate(row.createdAt)) },
  ];

  // Status options change based on type
  const statusOptions = typeFilter === 'MONEY_REQUEST'
    ? ['PENDING_APPROVAL', 'APPROVED', 'PROCESSING', 'COMPLETED', 'DECLINED', 'CANCELLED', 'FAILED']
    : ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'COMPENSATING', 'COMPENSATED'];

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">Transaction Monitor</h1>
        <p className="text-dark-400 mt-1">Real-time view of all transfers, bill payments, and money requests</p>
      </motion.div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        {/* Type tabs */}
        <div className="flex bg-dark-800 rounded-lg border border-dark-700 p-0.5">
          {[
            { value: 'TRANSFER', label: 'Transfers', icon: ArrowLeftRight },
            { value: 'BILL_PAYMENT', label: 'Bill Payments', icon: Receipt },
            { value: 'MONEY_REQUEST', label: 'Money Requests', icon: HandCoins },
          ].map(({ value, label, icon: Icon }) => (
            <button
              key={value}
              onClick={() => { setTypeFilter(value); setStatusFilter(''); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                typeFilter === value
                  ? 'bg-primary-600 text-white'
                  : 'text-dark-400 hover:text-dark-200'
              }`}
            >
              <Icon className="w-4 h-4" />
              {label}
            </button>
          ))}
        </div>

        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
          className="bg-dark-800 border border-dark-700 rounded-lg px-3 py-2.5 text-dark-300
                     focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Statuses</option>
          {statusOptions.map((s) => (
            <option key={s} value={s}>{s.replace('_', ' ')}</option>
          ))}
        </select>
      </div>

      {/* Data Table */}
      <DataTable<AdminTransaction>
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.page.totalPages ?? 0}
        totalElements={data?.page.totalElements ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        sorting={sorting}
        onSortingChange={setSorting}
        isLoading={isLoading}
        exportFilename={`finpay-${typeFilter.toLowerCase()}`}
        exportTitle={`FinPay - ${typeFilter.replace('_', ' ')} Report`}
        exportColumns={exportColumns}
      />
    </div>
  );
}
