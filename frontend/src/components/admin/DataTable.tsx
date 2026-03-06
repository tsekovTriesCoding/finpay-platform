import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
  type SortingState,
  type PaginationState,
  type OnChangeFn,
} from '@tanstack/react-table';
import { ChevronUp, ChevronDown, ChevronsUpDown, ChevronLeft, ChevronRight, Download, FileText } from 'lucide-react';

import { exportToCSV, exportToPDF } from '../../utils/exportUtils';

interface DataTableProps<T extends object> {
  data: T[];
  columns: ColumnDef<T, unknown>[];
  pageCount: number;
  totalElements: number;
  pagination: PaginationState;
  onPaginationChange: OnChangeFn<PaginationState>;
  sorting: SortingState;
  onSortingChange: OnChangeFn<SortingState>;
  isLoading?: boolean;
  exportFilename?: string;
  exportTitle?: string;
  exportColumns?: { header: string; accessor: keyof T | ((row: T) => string) }[];
}

export default function DataTable<T extends object>({
  data,
  columns,
  pageCount,
  totalElements,
  pagination,
  onPaginationChange,
  sorting,
  onSortingChange,
  isLoading = false,
  exportFilename,
  exportTitle,
  exportColumns,
}: DataTableProps<T>) {
  const table = useReactTable({
    data,
    columns,
    pageCount,
    state: { pagination, sorting },
    onPaginationChange,
    onSortingChange,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    manualSorting: true,
  });

  return (
    <div className="space-y-4">
      {/* Export buttons */}
      {exportColumns && exportFilename && (
        <div className="flex justify-end gap-2">
          <button
            onClick={() => exportToCSV(data, exportColumns, exportFilename)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg
                       bg-dark-800 text-gray-300 border border-dark-700 hover:bg-dark-700 transition-colors"
            disabled={data.length === 0}
          >
            <Download className="w-4 h-4" />
            CSV
          </button>
          <button
            onClick={() => exportToPDF(data, exportColumns, exportFilename, exportTitle)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg
                       bg-dark-800 text-gray-300 border border-dark-700 hover:bg-dark-700 transition-colors"
            disabled={data.length === 0}
          >
            <FileText className="w-4 h-4" />
            PDF
          </button>
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-dark-700">
        <table className="w-full text-sm text-left">
          <thead className="text-xs text-gray-400 uppercase bg-dark-800/50">
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  const sorted = header.column.getIsSorted();
                  return (
                    <th
                      key={header.id}
                      className="px-4 py-3 whitespace-nowrap"
                      onClick={header.column.getCanSort() ? header.column.getToggleSortingHandler() : undefined}
                      style={{ cursor: header.column.getCanSort() ? 'pointer' : 'default' }}
                    >
                      <div className="flex items-center gap-1">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {header.column.getCanSort() && (
                          <span className="text-gray-500">
                            {sorted === 'asc' ? (
                              <ChevronUp className="w-4 h-4" />
                            ) : sorted === 'desc' ? (
                              <ChevronDown className="w-4 h-4" />
                            ) : (
                              <ChevronsUpDown className="w-4 h-4" />
                            )}
                          </span>
                        )}
                      </div>
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>
          <tbody className="divide-y divide-dark-700">
            {isLoading ? (
              Array.from({ length: pagination.pageSize }).map((_, i) => (
                <tr key={i} className="animate-pulse">
                  {columns.map((_, j) => (
                    <td key={j} className="px-4 py-3">
                      <div className="h-4 bg-dark-700 rounded w-3/4" />
                    </td>
                  ))}
                </tr>
              ))
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-gray-500">
                  No records found
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr key={row.id} className="bg-dark-900 hover:bg-dark-800/50 transition-colors">
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-3 text-gray-300 whitespace-nowrap">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between text-sm text-gray-400">
        <div>
          Showing {pagination.pageIndex * pagination.pageSize + 1}–
          {Math.min((pagination.pageIndex + 1) * pagination.pageSize, totalElements)} of{' '}
          {totalElements.toLocaleString()} results
        </div>
        <div className="flex items-center gap-2">
          <select
            value={pagination.pageSize}
            onChange={(e) =>
              onPaginationChange((prev) => ({
                ...(typeof prev === 'function' ? prev : prev),
                pageIndex: 0,
                pageSize: Number(e.target.value),
              }))
            }
            className="bg-dark-800 border border-dark-700 rounded-lg px-2 py-1 text-gray-300
                       focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            {[10, 20, 50, 100].map((size) => (
              <option key={size} value={size}>
                {size} / page
              </option>
            ))}
          </select>

          <div className="flex gap-1">
            <button
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
              className="p-1.5 rounded-lg bg-dark-800 border border-dark-700 hover:bg-dark-700
                         disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-3 py-1.5 text-gray-300">
              Page {pagination.pageIndex + 1} of {Math.max(pageCount, 1)}
            </span>
            <button
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
              className="p-1.5 rounded-lg bg-dark-800 border border-dark-700 hover:bg-dark-700
                         disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
