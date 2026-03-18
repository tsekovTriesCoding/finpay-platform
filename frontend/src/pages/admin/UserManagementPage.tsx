import { useState, useMemo } from 'react';
import { type ColumnDef, type SortingState, type PaginationState } from '@tanstack/react-table';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Eye } from 'lucide-react';

import DataTable from '../../components/admin/DataTable';
import ConfirmModal, { type ConfirmAction } from '../../components/admin/ConfirmModal';
import UserDetailPanel from '../../components/admin/UserDetailPanel';
import { STATUS_COLORS, ROLE_COLORS } from '../../components/admin/constants';
import { useAdminUsers } from '../../hooks/useAdmin';
import type { AdminUser } from '../../api/adminApi';
import { formatDate } from '../../utils/exportUtils';

export default function UserManagementPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [sorting, setSorting] = useState<SortingState>([{ id: 'createdAt', desc: true }]);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction | null>(null);

  const sortBy = sorting[0]?.id || 'createdAt';
  const sortDir = sorting[0]?.desc ? 'desc' : 'asc';

  const { data, isLoading } = useAdminUsers({
    search: search || undefined,
    status: statusFilter || undefined,
    role: roleFilter || undefined,
    sortBy,
    sortDir: sortDir as 'asc' | 'desc',
    page: pagination.pageIndex,
    size: pagination.pageSize,
  });

  const handleConfirmAction = (action: ConfirmAction) => {
    setConfirmAction(action);
  };

  const handleConfirmDone = () => {
    setConfirmAction(null);
    setSelectedUser(null);
  };

  const columns = useMemo<ColumnDef<AdminUser, unknown>[]>(
    () => [
      {
        accessorKey: 'email',
        header: 'Email',
        cell: ({ row }) => (
          <div>
            <div className="font-medium text-white">{row.original.email}</div>
            <div className="text-xs text-gray-500">
              {row.original.firstName} {row.original.lastName}
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'role',
        header: 'Role',
        cell: ({ getValue }) => {
          const role = getValue() as string;
          return (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ROLE_COLORS[role] || ''}`}>
              {role}
            </span>
          );
        },
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ getValue }) => {
          const status = getValue() as string;
          return (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[status] || ''}`}>
              {status.replace('_', ' ')}
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
        accessorKey: 'emailVerified',
        header: 'Verified',
        cell: ({ getValue }) => (
          <span className={getValue() ? 'text-green-400' : 'text-red-400'}>
            {getValue() ? 'Yes' : 'No'}
          </span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Created',
        cell: ({ getValue }) => <span className="text-gray-400">{formatDate(getValue() as string)}</span>,
      },
      {
        accessorKey: 'lastLoginAt',
        header: 'Last Login',
        cell: ({ getValue }) => (
          <span className="text-gray-400">{formatDate(getValue() as string | null)}</span>
        ),
      },
      {
        id: 'actions',
        header: '',
        enableSorting: false,
        cell: ({ row }) => (
          <button
            onClick={() => setSelectedUser(row.original)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-primary-400
                       bg-primary-500/10 hover:bg-primary-500/20 rounded-lg transition-colors"
          >
            <Eye className="w-3.5 h-3.5" />
            View
          </button>
        ),
      },
    ],
    []
  );

  const exportColumns = [
    { header: 'Email', accessor: 'email' as const },
    { header: 'Name', accessor: ((row: AdminUser) => `${row.firstName} ${row.lastName}`) },
    { header: 'Role', accessor: 'role' as const },
    { header: 'Status', accessor: 'status' as const },
    { header: 'Plan', accessor: 'plan' as const },
    { header: 'Verified', accessor: ((row: AdminUser) => row.emailVerified ? 'Yes' : 'No') },
    { header: 'Created', accessor: ((row: AdminUser) => formatDate(row.createdAt)) },
    { header: 'Last Login', accessor: ((row: AdminUser) => formatDate(row.lastLoginAt)) },
  ];

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">User Management</h1>
        <p className="text-dark-400 mt-1">Manage platform users, roles, and account status</p>
      </motion.div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-500" />
          <input
            type="text"
            placeholder="Search by name or email..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
            className="w-full pl-10 pr-4 py-2.5 bg-dark-800 border border-dark-700 rounded-lg text-dark-200
                       placeholder:text-dark-500 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
          className="bg-dark-800 border border-dark-700 rounded-lg px-3 py-2.5 text-dark-300
                     focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="PENDING_VERIFICATION">Pending Verification</option>
        </select>
        <select
          value={roleFilter}
          onChange={(e) => { setRoleFilter(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
          className="bg-dark-800 border border-dark-700 rounded-lg px-3 py-2.5 text-dark-300
                     focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Roles</option>
          <option value="USER">User</option>
          <option value="ADMIN">Admin</option>
          <option value="MERCHANT">Merchant</option>
        </select>
      </div>

      {/* Data Table */}
      <DataTable<AdminUser>
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.page.totalPages ?? 0}
        totalElements={data?.page.totalElements ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        sorting={sorting}
        onSortingChange={setSorting}
        isLoading={isLoading}
        exportFilename="finpay-users"
        exportTitle="FinPay - User Management Report"
        exportColumns={exportColumns}
      />

      {/* Slide-over detail panel */}
      <AnimatePresence>
        {selectedUser && (
          <UserDetailPanel
            user={selectedUser}
            onClose={() => setSelectedUser(null)}
            onAction={handleConfirmAction}
          />
        )}
      </AnimatePresence>

      {/* Confirmation modal */}
      <AnimatePresence>
        {confirmAction && (
          <ConfirmModal
            action={confirmAction}
            onClose={handleConfirmDone}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
