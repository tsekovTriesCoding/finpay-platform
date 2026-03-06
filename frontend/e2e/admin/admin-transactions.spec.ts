import { test, expect, makeTransaction, makePage } from './admin-fixtures';

const API_BASE = 'http://localhost:8080';

test.describe('Transaction Monitor Page', () => {
  test.beforeEach(async ({ adminPage: page }) => {
    await page.goto('/admin/transactions');
    await expect(page.locator('h1:has-text("Transaction Monitor")')).toBeVisible();
  });

  // Page structure
  test('displays page header and description', async ({ adminPage: page }) => {
    await expect(page.locator('text=Real-time view of all transfers, bill payments, and money requests')).toBeVisible();
  });

  // Type tabs
  test('displays transaction type tabs', async ({ adminPage: page }) => {
    await expect(page.locator('button:has-text("Transfers")')).toBeVisible();
    await expect(page.locator('button:has-text("Bill Payments")')).toBeVisible();
    await expect(page.locator('button:has-text("Money Requests")')).toBeVisible();
  });

  test('Transfers tab is active by default', async ({ adminPage: page }) => {
    const transferTab = page.locator('button:has-text("Transfers")');
    await expect(transferTab).toHaveClass(/bg-primary-600/);
  });

  // Status filter
  test('displays status filter dropdown', async ({ adminPage: page }) => {
    await expect(page.locator('select:has(option:has-text("All Statuses"))')).toBeVisible();
  });

  test('status filter shows transfer-specific options for Transfers tab', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Statuses"))');
    await expect(select.locator('option:has-text("COMPLETED")')).toBeAttached();
    await expect(select.locator('option:has-text("PENDING")')).toBeAttached();
    await expect(select.locator('option:has-text("FAILED")')).toBeAttached();
  });

  // Column headers
  test('displays correct table column headers', async ({ adminPage: page }) => {
    await expect(page.locator('th:has-text("Type")')).toBeVisible();
    await expect(page.locator('th:has-text("Reference")')).toBeVisible();
    await expect(page.locator('th:has-text("Amount")')).toBeVisible();
    await expect(page.locator('th:has-text("Status")')).toBeVisible();
    await expect(page.locator('th:has-text("From")')).toBeVisible();
    await expect(page.locator('th:has-text("To")')).toBeVisible();
    await expect(page.locator('th:has-text("Description")')).toBeVisible();
    await expect(page.locator('th:has-text("Date")')).toBeVisible();
  });

  // Data rendering
  test('renders transaction data in table', async ({ adminPage: page }) => {
    // Verify at least some transaction data is visible
    await expect(page.locator('text=Monthly rent')).toBeVisible();
    await expect(page.locator('text=Utility bill')).toBeVisible();
  });

  test('shows status badges with colors', async ({ adminPage: page }) => {
    await expect(page.locator('span:has-text("COMPLETED")')).toBeVisible();
    await expect(page.locator('span:has-text("PENDING")')).toBeVisible();
    await expect(page.locator('span:has-text("FAILED")')).toBeVisible();
  });

  // Export
  test('displays CSV and PDF export buttons', async ({ adminPage: page }) => {
    await expect(page.locator('button:has-text("CSV")')).toBeVisible();
    await expect(page.locator('button:has-text("PDF")')).toBeVisible();
  });

  // Pagination
  test('shows pagination info', async ({ adminPage: page }) => {
    await expect(page.locator('text=/Showing.*of.*results/')).toBeVisible();
  });

  // Tab switching
  test('switching to Bill Payments tab triggers API call with type param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/transactions?*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([
          makeTransaction({ type: 'BILL_PAYMENT', description: 'Electric bill', status: 'COMPLETED' }),
        ])),
      });
    });

    await page.locator('button:has-text("Bill Payments")').click();
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('type=BILL_PAYMENT');
  });

  test('switching to Money Requests shows different status options', async ({ adminPage: page }) => {
    await page.route(`${API_BASE}/api/v1/admin/transactions?*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([
          makeTransaction({ type: 'MONEY_REQUEST', status: 'PENDING_APPROVAL', description: 'Split dinner' }),
        ])),
      }),
    );

    await page.locator('button:has-text("Money Requests")').click();
    await page.waitForTimeout(300);

    const select = page.locator('select:has(option:has-text("All Statuses"))');
    await expect(select.locator('option:has-text("PENDING APPROVAL")')).toBeAttached();
    await expect(select.locator('option:has-text("DECLINED")')).toBeAttached();
  });

  test('status filter triggers API call with status param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/transactions?*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([
          makeTransaction({ status: 'FAILED', description: 'Failed payment' }),
        ])),
      });
    });

    await page.locator('select:has(option:has-text("All Statuses"))').selectOption('FAILED');
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('status=FAILED');
  });

  // Empty state
  test('shows empty state when no transactions match', async ({ adminPage: page }) => {
    await page.route(`${API_BASE}/api/v1/admin/transactions?*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([])),
      }),
    );

    await page.locator('select:has(option:has-text("All Statuses"))').selectOption('FAILED');
    await page.waitForTimeout(300);
    await expect(page.locator('text=No records found')).toBeVisible();
  });
});
