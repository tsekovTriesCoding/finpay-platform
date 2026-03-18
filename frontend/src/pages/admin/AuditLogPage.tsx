import { useState, useMemo } from 'react';
import { type ColumnDef, type SortingState, type PaginationState } from '@tanstack/react-table';
import { motion } from 'framer-motion';

import DataTable from '../../components/admin/DataTable';
import { useAdminAuditLogs } from '../../hooks/useAdmin';
import type { AuditLogEntry } from '../../api/adminApi';
import { formatDate } from '../../utils/exportUtils';

const ACTION_COLORS: Record<string, string> = {
  USER_ROLE_CHANGED: 'bg-purple-500/20 text-purple-400',
  USER_SUSPENDED: 'bg-red-500/20 text-red-400',
  USER_UNSUSPENDED: 'bg-green-500/20 text-green-400',
  USER_DELETED: 'bg-red-500/20 text-red-300',
  USER_FORCE_PASSWORD_RESET: 'bg-yellow-500/20 text-yellow-400',
  USER_IMPERSONATED: 'bg-orange-500/20 text-orange-400',
  WALLET_FROZEN: 'bg-blue-500/20 text-blue-400',
  WALLET_UNFROZEN: 'bg-green-500/20 text-green-300',
  TRANSACTION_FLAGGED: 'bg-red-500/20 text-red-400',
  NOTIFICATION_BROADCAST_SENT: 'bg-blue-500/20 text-blue-300',
};

export default function AuditLogPage() {
  const [actionFilter, setActionFilter] = useState('');
  const [targetTypeFilter, setTargetTypeFilter] = useState('');
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [sorting, setSorting] = useState<SortingState>([{ id: 'createdAt', desc: true }]);

  const { data, isLoading } = useAdminAuditLogs({
    action: actionFilter || undefined,
    targetType: targetTypeFilter || undefined,
    page: pagination.pageIndex,
    size: pagination.pageSize,
  });

  const columns = useMemo<ColumnDef<AuditLogEntry, unknown>[]>(
    () => [
      {
        accessorKey: 'createdAt',
        header: 'Timestamp',
        cell: ({ getValue }) => (
          <span className="text-gray-400 text-xs">{formatDate(getValue() as string)}</span>
        ),
      },
      {
        accessorKey: 'actorEmail',
        header: 'Actor',
        cell: ({ getValue }) => <span className="text-white font-medium">{getValue() as string}</span>,
      },
      {
        accessorKey: 'action',
        header: 'Action',
        cell: ({ getValue }) => {
          const action = getValue() as string;
          return (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ACTION_COLORS[action] || 'bg-gray-500/20 text-gray-400'}`}>
              {action.replace(/_/g, ' ')}
            </span>
          );
        },
      },
      {
        accessorKey: 'targetType',
        header: 'Target Type',
        cell: ({ getValue }) => <span className="text-gray-300 text-xs">{getValue() as string}</span>,
      },
      {
        accessorKey: 'targetId',
        header: 'Target ID',
        cell: ({ getValue }) => (
          <code className="text-xs text-gray-400 font-mono">
            {(getValue() as string).length > 12
              ? (getValue() as string).substring(0, 12) + '...'
              : (getValue() as string)}
          </code>
        ),
      },
      {
        accessorKey: 'description',
        header: 'Description',
        enableSorting: false,
        cell: ({ getValue }) => (
          <span className="text-gray-400 max-w-[300px] truncate block text-xs">
            {getValue() as string}
          </span>
        ),
      },
      {
        accessorKey: 'ipAddress',
        header: 'IP Address',
        enableSorting: false,
        cell: ({ getValue }) => (
          <code className="text-xs text-gray-500 font-mono">{(getValue() as string) || '—'}</code>
        ),
      },
      {
        accessorKey: 'serviceSource',
        header: 'Service',
        enableSorting: false,
        cell: ({ getValue }) => <span className="text-gray-500 text-xs">{getValue() as string}</span>,
      },
    ],
    []
  );

  const exportColumns = [
    { header: 'Timestamp', accessor: ((row: AuditLogEntry) => formatDate(row.createdAt)) },
    { header: 'Actor', accessor: 'actorEmail' as const },
    { header: 'Action', accessor: 'action' as const },
    { header: 'Target Type', accessor: 'targetType' as const },
    { header: 'Target ID', accessor: 'targetId' as const },
    { header: 'Description', accessor: 'description' as const },
    { header: 'IP Address', accessor: ((row: AuditLogEntry) => row.ipAddress || '') },
    { header: 'Service', accessor: 'serviceSource' as const },
  ];

  const auditActions = [
    'USER_ROLE_CHANGED', 'USER_SUSPENDED', 'USER_UNSUSPENDED', 'USER_DELETED',
    'USER_FORCE_PASSWORD_RESET', 'USER_IMPERSONATED',
    'WALLET_FROZEN', 'WALLET_UNFROZEN', 'WALLET_CLOSED',
    'TRANSACTION_FLAGGED', 'TRANSACTION_UNFLAGGED',
    'NOTIFICATION_BROADCAST_SENT', 'BULK_ACTION_EXECUTED',
  ];

  const targetTypes = ['USER', 'WALLET', 'TRANSFER', 'BILL_PAYMENT', 'MONEY_REQUEST', 'PAYMENT', 'NOTIFICATION', 'SYSTEM'];

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">Audit Logs</h1>
        <p className="text-dark-400 mt-1">Compliance audit trail - WHO did WHAT to WHICH resource WHEN</p>
      </motion.div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <select
          value={actionFilter}
          onChange={(e) => { setActionFilter(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
          className="bg-dark-800 border border-dark-700 rounded-lg px-3 py-2.5 text-dark-300
                     focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Actions</option>
          {auditActions.map((a) => (
            <option key={a} value={a}>{a.replace(/_/g, ' ')}</option>
          ))}
        </select>
        <select
          value={targetTypeFilter}
          onChange={(e) => { setTargetTypeFilter(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
          className="bg-dark-800 border border-dark-700 rounded-lg px-3 py-2.5 text-dark-300
                     focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Target Types</option>
          {targetTypes.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
      </div>

      <DataTable<AuditLogEntry>
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.page.totalPages ?? 0}
        totalElements={data?.page.totalElements ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        sorting={sorting}
        onSortingChange={setSorting}
        isLoading={isLoading}
        exportFilename="finpay-audit-logs"
        exportTitle="FinPay - Audit Log Report"
        exportColumns={exportColumns}
      />
    </div>
  );
}
