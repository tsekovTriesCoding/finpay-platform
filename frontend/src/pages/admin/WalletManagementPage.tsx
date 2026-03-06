import { useState, useMemo } from 'react';
import { type ColumnDef, type SortingState, type PaginationState } from '@tanstack/react-table';
import { motion } from 'framer-motion';
import { Snowflake, Sun } from 'lucide-react';

import DataTable from '../../components/admin/DataTable';
import { useAdminWallets, useFreezeWallet, useUnfreezeWallet } from '../../hooks/useAdmin';
import type { AdminWallet } from '../../api/adminApi';
import { formatDate, formatCurrency } from '../../utils/exportUtils';

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-green-500/20 text-green-400',
  FROZEN: 'bg-blue-500/20 text-blue-400',
  CLOSED: 'bg-gray-500/20 text-gray-400',
};

export default function WalletManagementPage() {
  const [statusFilter, setStatusFilter] = useState('');
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [sorting, setSorting] = useState<SortingState>([{ id: 'createdAt', desc: true }]);

  const sortBy = sorting[0]?.id || 'createdAt';
  const sortDir = sorting[0]?.desc ? 'desc' : 'asc';

  const { data, isLoading } = useAdminWallets({
    status: statusFilter || undefined,
    sortBy,
    sortDir: sortDir as 'asc' | 'desc',
    page: pagination.pageIndex,
    size: pagination.pageSize,
  });

  const freezeWallet = useFreezeWallet();
  const unfreezeWallet = useUnfreezeWallet();

  const columns = useMemo<ColumnDef<AdminWallet, unknown>[]>(
    () => [
      {
        accessorKey: 'userId',
        header: 'User ID',
        cell: ({ getValue }) => (
          <code className="text-xs text-gray-400 font-mono">{(getValue() as string).substring(0, 12)}...</code>
        ),
      },
      {
        accessorKey: 'balance',
        header: 'Balance',
        cell: ({ row }) => (
          <span className="font-medium text-white">
            {formatCurrency(row.original.balance, row.original.currency)}
          </span>
        ),
      },
      {
        accessorKey: 'availableBalance',
        header: 'Available',
        cell: ({ row }) => (
          <span className="text-gray-300">
            {formatCurrency(row.original.availableBalance, row.original.currency)}
          </span>
        ),
      },
      {
        accessorKey: 'reservedBalance',
        header: 'Reserved',
        cell: ({ row }) => (
          <span className="text-yellow-400">{formatCurrency(row.original.reservedBalance)}</span>
        ),
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ getValue }) => {
          const status = getValue() as string;
          return (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[status] || ''}`}>
              {status}
            </span>
          );
        },
      },
      {
        accessorKey: 'plan',
        header: 'Plan',
        cell: ({ getValue }) => <span className="text-gray-300">{getValue() as string}</span>,
      },
      {
        accessorKey: 'dailySpent',
        header: 'Daily Spent',
        cell: ({ row }) => (
          <span className="text-gray-400">
            {formatCurrency(row.original.dailySpent)} / {formatCurrency(row.original.dailyTransactionLimit)}
          </span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Created',
        cell: ({ getValue }) => (
          <span className="text-gray-400 text-xs">{formatDate(getValue() as string)}</span>
        ),
      },
      {
        id: 'actions',
        header: 'Actions',
        enableSorting: false,
        cell: ({ row }) => {
          const wallet = row.original;
          if (wallet.status === 'ACTIVE') {
            return (
              <button
                onClick={() => freezeWallet.mutate(wallet.userId)}
                disabled={freezeWallet.isPending}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-lg
                           bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 transition-colors
                           disabled:opacity-50"
              >
                <Snowflake className="w-3.5 h-3.5" />
                Freeze
              </button>
            );
          }
          if (wallet.status === 'FROZEN') {
            return (
              <button
                onClick={() => unfreezeWallet.mutate(wallet.userId)}
                disabled={unfreezeWallet.isPending}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-lg
                           bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors
                           disabled:opacity-50"
              >
                <Sun className="w-3.5 h-3.5" />
                Unfreeze
              </button>
            );
          }
          return <span className="text-gray-600">—</span>;
        },
      },
    ],
    [freezeWallet, unfreezeWallet]
  );

  const exportColumns = [
    { header: 'User ID', accessor: 'userId' as const },
    { header: 'Balance', accessor: ((row: AdminWallet) => formatCurrency(row.balance, row.currency)) },
    { header: 'Available', accessor: ((row: AdminWallet) => formatCurrency(row.availableBalance, row.currency)) },
    { header: 'Reserved', accessor: ((row: AdminWallet) => formatCurrency(row.reservedBalance)) },
    { header: 'Status', accessor: 'status' as const },
    { header: 'Plan', accessor: 'plan' as const },
    { header: 'Created', accessor: ((row: AdminWallet) => formatDate(row.createdAt)) },
  ];

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">Wallet Management</h1>
        <p className="text-dark-400 mt-1">View and manage all platform wallets, freeze/unfreeze accounts</p>
      </motion.div>

      {/* Filters */}
      <div className="flex gap-3">
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
          className="bg-dark-800 border border-dark-700 rounded-lg px-3 py-2.5 text-dark-300
                     focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="FROZEN">Frozen</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>

      <DataTable<AdminWallet>
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.totalPages ?? 0}
        totalElements={data?.totalElements ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        sorting={sorting}
        onSortingChange={setSorting}
        isLoading={isLoading}
        exportFilename="finpay-wallets"
        exportTitle="FinPay - Wallet Management Report"
        exportColumns={exportColumns}
      />
    </div>
  );
}
