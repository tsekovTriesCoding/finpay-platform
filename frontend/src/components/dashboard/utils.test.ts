import { describe, it, expect } from 'vitest';
import { formatCurrency, statusBadgeClasses } from './utils';

describe('formatCurrency', () => {
  it('formats whole numbers with two decimal places', () => {
    expect(formatCurrency(1000)).toBe('$1,000.00');
  });

  it('formats decimals correctly', () => {
    expect(formatCurrency(12345.67)).toBe('$12,345.67');
  });

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('formats small amounts', () => {
    expect(formatCurrency(0.5)).toBe('$0.50');
  });

  it('formats large amounts', () => {
    expect(formatCurrency(1000000)).toBe('$1,000,000.00');
  });
});

describe('statusBadgeClasses', () => {
  it('returns secondary/green classes for COMPLETED status', () => {
    const classes = statusBadgeClasses('COMPLETED');
    expect(classes).toContain('secondary');
  });

  it('returns yellow classes for PROCESSING status (default branch)', () => {
    const classes = statusBadgeClasses('PROCESSING');
    expect(classes).toContain('yellow');
  });

  it('returns yellow classes for PENDING status (default branch)', () => {
    const classes = statusBadgeClasses('PENDING');
    expect(classes).toContain('yellow');
  });

  it('returns red classes for FAILED status', () => {
    const classes = statusBadgeClasses('FAILED');
    expect(classes).toContain('red');
  });

  it('returns red classes for COMPENSATED status', () => {
    const classes = statusBadgeClasses('COMPENSATED');
    expect(classes).toContain('red');
  });

  it('returns red classes for REFUNDED status', () => {
    const classes = statusBadgeClasses('REFUNDED');
    expect(classes).toContain('red');
  });

  it('returns dark classes for CANCELLED status', () => {
    const classes = statusBadgeClasses('CANCELLED');
    expect(classes).toContain('dark');
  });

  it('returns yellow (default) classes for unknown status', () => {
    const classes = statusBadgeClasses('SOME_UNKNOWN_STATUS');
    expect(classes).toContain('yellow');
  });
});
