import { test, expect, makeWallet, makePage } from './admin-fixtures';

const API_BASE = 'http://localhost:8080';

test.describe('Wallet Management Page', () => {
  test.beforeEach(async ({ adminPage: page }) => {
    await page.goto('/admin/wallets');
    await expect(page.locator('h1:has-text("Wallet Management")')).toBeVisible();
  });

  // Page structure
  test('displays page header and description', async ({ adminPage: page }) => {
    await expect(page.locator('text=View and manage all platform wallets, freeze/unfreeze accounts')).toBeVisible();
  });

  // Status filter
  test('displays status filter dropdown', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Statuses"))');
    await expect(select).toBeVisible();
    await expect(select.locator('option:has-text("Active")')).toBeAttached();
    await expect(select.locator('option:has-text("Frozen")')).toBeAttached();
    await expect(select.locator('option:has-text("Closed")')).toBeAttached();
  });

  // Column headers
  test('displays correct table column headers', async ({ adminPage: page }) => {
    await expect(page.locator('th:has-text("User ID")')).toBeVisible();
    await expect(page.locator('th:has-text("Balance")')).toBeVisible();
    await expect(page.locator('th:has-text("Available")')).toBeVisible();
    await expect(page.locator('th:has-text("Reserved")')).toBeVisible();
    await expect(page.locator('th:has-text("Status")')).toBeVisible();
    await expect(page.locator('th:has-text("Plan")')).toBeVisible();
    await expect(page.locator('th:has-text("Daily Spent")')).toBeVisible();
    await expect(page.locator('th:has-text("Created")')).toBeVisible();
    await expect(page.locator('th:has-text("Actions")')).toBeVisible();
  });

  // Data rendering
  test('renders wallet data in table', async ({ adminPage: page }) => {
    // Status badges should be visible
    await expect(page.locator('span:has-text("ACTIVE")').first()).toBeVisible();
    await expect(page.locator('span:has-text("FROZEN")')).toBeVisible();
    await expect(page.locator('span:has-text("CLOSED")')).toBeVisible();
  });

  // Freeze / Unfreeze buttons
  test('active wallet shows Freeze button', async ({ adminPage: page }) => {
    await expect(page.locator('span:has-text("ACTIVE")').first()).toBeVisible();
    const activeRow = page.locator('tr:has(span:has-text("ACTIVE"))').first();
    await expect(activeRow.locator('button:has-text("Freeze")')).toBeVisible();
  });

  test('frozen wallet shows Unfreeze button', async ({ adminPage: page }) => {
    const frozenRow = page.locator('tr:has(span:has-text("FROZEN"))');
    await expect(frozenRow.locator('button:has-text("Unfreeze")')).toBeVisible();
  });

  test('closed wallet has no action button', async ({ adminPage: page }) => {
    const closedRow = page.locator('tr:has(span:has-text("CLOSED"))');
    // Should show "—" instead of a button
    await expect(closedRow.locator('button')).toHaveCount(0);
  });

  // Freeze action
  test('clicking Freeze calls the freeze API', async ({ adminPage: page }) => {
    let freezeCalled = false;
    await page.route(`${API_BASE}/api/v1/admin/wallets/user/*/freeze`, (route) => {
      freezeCalled = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makeWallet({ status: 'FROZEN' })),
      });
    });

    const activeRow = page.locator('tr:has(span:has-text("ACTIVE"))').first();
    await activeRow.locator('button:has-text("Freeze")').click();

    await page.waitForTimeout(500);
    expect(freezeCalled).toBe(true);
  });

  // Unfreeze action
  test('clicking Unfreeze calls the unfreeze API', async ({ adminPage: page }) => {
    let unfreezeCalled = false;
    await page.route(`${API_BASE}/api/v1/admin/wallets/user/*/unfreeze`, (route) => {
      unfreezeCalled = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makeWallet({ status: 'ACTIVE' })),
      });
    });

    const frozenRow = page.locator('tr:has(span:has-text("FROZEN"))');
    await frozenRow.locator('button:has-text("Unfreeze")').click();

    await page.waitForTimeout(500);
    expect(unfreezeCalled).toBe(true);
  });

  // Status filter API
  test('status filter triggers API call with status param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/wallets?*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([makeWallet({ status: 'FROZEN' })])),
      });
    });

    await page.locator('select:has(option:has-text("All Statuses"))').selectOption('FROZEN');
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('status=FROZEN');
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

  // Empty state
  test('shows empty state when no wallets match', async ({ adminPage: page }) => {
    await page.route(`${API_BASE}/api/v1/admin/wallets?*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([])),
      }),
    );

    await page.locator('select:has(option:has-text("All Statuses"))').selectOption('CLOSED');
    await page.waitForTimeout(300);
    await expect(page.locator('text=No records found')).toBeVisible();
  });
});
