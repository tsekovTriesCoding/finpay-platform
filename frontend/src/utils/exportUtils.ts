import { saveAs } from 'file-saver';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

/**
 * Export data to CSV file.
 */
export function exportToCSV<T extends object>(
  data: T[],
  columns: { header: string; accessor: keyof T | ((row: T) => string) }[],
  filename: string
) {
  const headers = columns.map((col) => col.header);
  const rows = data.map((row) =>
    columns.map((col) => {
      const value = typeof col.accessor === 'function'
        ? col.accessor(row)
        : String(row[col.accessor] ?? '');
      // Escape CSV special characters
      if (value.includes(',') || value.includes('"') || value.includes('\n')) {
        return `"${value.replace(/"/g, '""')}"`;
      }
      return value;
    })
  );

  const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  saveAs(blob, `${filename}.csv`);
}

/**
 * Export data to PDF file.
 */
export function exportToPDF<T extends object>(
  data: T[],
  columns: { header: string; accessor: keyof T | ((row: T) => string) }[],
  filename: string,
  title?: string
) {
  const doc = new jsPDF();

  // Title
  if (title) {
    doc.setFontSize(16);
    doc.text(title, 14, 22);
    doc.setFontSize(10);
    doc.text(`Generated: ${new Date().toLocaleString()}`, 14, 30);
  }

  const headers = columns.map((col) => col.header);
  const rows = data.map((row) =>
    columns.map((col) =>
      typeof col.accessor === 'function'
        ? col.accessor(row)
        : String(row[col.accessor] ?? '')
    )
  );

  autoTable(doc, {
    head: [headers],
    body: rows,
    startY: title ? 35 : 20,
    styles: { fontSize: 8 },
    headStyles: { fillColor: [37, 99, 235] }, // blue-600
  });

  doc.save(`${filename}.pdf`);
}

/**
 * Format currency value for display.
 */
export function formatCurrency(amount: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

/**
 * Format date for display.
 */
export function formatDate(dateStr: string | null): string {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
