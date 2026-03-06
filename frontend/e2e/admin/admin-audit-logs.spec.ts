import { test, expect, makeAuditLog, makePage } from './admin-fixtures';

const API_BASE = 'http://localhost:8080';

test.describe('Audit Log Page', () => {
  test.beforeEach(async ({ adminPage: page }) => {
    await page.goto('/admin/audit-logs');
    await expect(page.locator('h1:has-text("Audit Logs")')).toBeVisible();
  });

  // Page structure
  test('displays page header and description', async ({ adminPage: page }) => {
    await expect(page.locator('text=Compliance audit trail')).toBeVisible();
  });

  // Filters
  test('displays action filter dropdown', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Actions"))');
    await expect(select).toBeVisible();
  });

  test('action filter has all audit actions', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Actions"))');
    await expect(select.locator('option:has-text("USER ROLE CHANGED")')).toBeAttached();
    await expect(select.locator('option:has-text("USER SUSPENDED")')).toBeAttached();
    await expect(select.locator('option:has-text("USER UNSUSPENDED")')).toBeAttached();
    await expect(select.locator('option:has-text("WALLET FROZEN")')).toBeAttached();
    await expect(select.locator('option:has-text("WALLET UNFROZEN")')).toBeAttached();
  });

  test('displays target type filter dropdown', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Target Types"))');
    await expect(select).toBeVisible();
    await expect(select.locator('option:has-text("USER")')).toBeAttached();
    await expect(select.locator('option:has-text("WALLET")')).toBeAttached();
    await expect(select.locator('option:has-text("SYSTEM")')).toBeAttached();
  });

  // Column headers
  test('displays correct table column headers', async ({ adminPage: page }) => {
    await expect(page.locator('th:has-text("Timestamp")')).toBeVisible();
    await expect(page.locator('th:has-text("Actor")')).toBeVisible();
    await expect(page.locator('th:has-text("Action")')).toBeVisible();
    await expect(page.locator('th:has-text("Target Type")')).toBeVisible();
    await expect(page.locator('th:has-text("Target ID")')).toBeVisible();
    await expect(page.locator('th:has-text("Description")')).toBeVisible();
    await expect(page.locator('th:has-text("IP Address")')).toBeVisible();
    await expect(page.locator('th:has-text("Service")')).toBeVisible();
  });

  // Data rendering
  test('renders audit log data in table', async ({ adminPage: page }) => {
    await expect(page.locator('text=admin@finpay.com').first()).toBeVisible();
    await expect(page.locator('text=user-service').first()).toBeVisible();
  });

  test('shows action badges with formatted labels', async ({ adminPage: page }) => {
    await expect(page.locator('span:has-text("USER SUSPENDED")')).toBeVisible();
    await expect(page.locator('span:has-text("USER ROLE CHANGED")')).toBeVisible();
    await expect(page.locator('span:has-text("WALLET FROZEN")')).toBeVisible();
  });

  test('shows IP address for audit entries', async ({ adminPage: page }) => {
    await expect(page.locator('text=192.168.1.100').first()).toBeVisible();
  });

  // Action filter API call
  test('action filter triggers API call with action param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/users/audit-logs/search*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([
          makeAuditLog({ action: 'WALLET_FROZEN', description: 'Froze wallet' }),
        ])),
      });
    });

    await page.locator('select:has(option:has-text("All Actions"))').selectOption('WALLET_FROZEN');
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('action=WALLET_FROZEN');
  });

  // Target type filter API call
  test('target type filter triggers API call with targetType param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/users/audit-logs/search*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([
          makeAuditLog({ action: 'WALLET_FROZEN', targetType: 'WALLET' }),
        ])),
      });
    });

    await page.locator('select:has(option:has-text("All Target Types"))').selectOption('WALLET');
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('targetType=WALLET');
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
  test('shows empty state when no audit logs match', async ({ adminPage: page }) => {
    await page.route(`${API_BASE}/api/v1/admin/users/audit-logs/search*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([])),
      }),
    );

    await page.locator('select:has(option:has-text("All Actions"))').selectOption('USER_DELETED');
    await page.waitForTimeout(300);
    await expect(page.locator('text=No records found')).toBeVisible();
  });
});
