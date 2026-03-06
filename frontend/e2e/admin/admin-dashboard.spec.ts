import { test, expect, DASHBOARD_METRICS, TRANSACTION_METRICS, WALLET_METRICS } from './admin-fixtures';

test.describe('Admin Dashboard Page', () => {
  test.beforeEach(async ({ adminPage: page }) => {
    await page.goto('/admin');
    await expect(page.locator('h1:has-text("Admin Dashboard")')).toBeVisible();
  });

  test('displays page header', async ({ adminPage: page }) => {
    await expect(page.locator('text=Platform overview and KPI metrics')).toBeVisible();
  });

  // User Metrics row
  test('displays user KPI cards', async ({ adminPage: page }) => {
    await expect(page.locator('text=User Metrics')).toBeVisible();
    await expect(page.locator('text=Total Users')).toBeVisible();
    await expect(page.locator('text=Active Users')).toBeVisible();
    await expect(page.locator('text=Suspended')).toBeVisible();
    await expect(page.locator('text=Pending Verification')).toBeVisible();
  });

  test('user KPI values match mock data', async ({ adminPage: page }) => {
    await expect(page.getByText(DASHBOARD_METRICS.totalUsers.toLocaleString(), { exact: true })).toBeVisible();
    await expect(page.getByText(DASHBOARD_METRICS.activeUsers.toLocaleString(), { exact: true })).toBeVisible();
    await expect(page.getByText(DASHBOARD_METRICS.suspendedUsers.toLocaleString(), { exact: true })).toBeVisible();
    await expect(page.getByText(DASHBOARD_METRICS.pendingVerification.toLocaleString(), { exact: true })).toBeVisible();
  });

  // Transaction Volume row
  test('displays transaction KPI cards', async ({ adminPage: page }) => {
    await expect(page.locator('text=Transaction Volume')).toBeVisible();
    await expect(page.locator('text=Total Transfers')).toBeVisible();
    await expect(page.locator('text=Transfer Volume')).toBeVisible();
    await expect(page.locator('text=Failed Transfers')).toBeVisible();
    await expect(page.locator('text=Bill Payments')).toBeVisible();
  });

  test('transaction KPI values match mock data', async ({ adminPage: page }) => {
    await expect(page.getByText(TRANSACTION_METRICS.totalTransfers.toLocaleString(), { exact: true })).toBeVisible();
    await expect(page.getByText(TRANSACTION_METRICS.failedTransfers.toLocaleString(), { exact: true }).first()).toBeVisible();
    await expect(page.getByText(TRANSACTION_METRICS.totalBillPayments.toLocaleString(), { exact: true })).toBeVisible();
  });

  // Wallets & Activity row
  test('displays wallet and activity KPI cards', async ({ adminPage: page }) => {
    await expect(page.locator('text=Wallets & Activity')).toBeVisible();
    await expect(page.locator('text=Total Wallets')).toBeVisible();
    await expect(page.locator('text=Frozen Wallets')).toBeVisible();
    await expect(page.locator('text=Platform Balance')).toBeVisible();
    await expect(page.locator('text=Admin Actions (24h)')).toBeVisible();
  });

  test('wallet KPI values match mock data', async ({ adminPage: page }) => {
    await expect(page.getByText(WALLET_METRICS.totalWallets.toLocaleString(), { exact: true })).toBeVisible();
    await expect(page.getByText(WALLET_METRICS.frozenWallets.toLocaleString(), { exact: true })).toBeVisible();
    await expect(page.getByText(DASHBOARD_METRICS.recentAuditActions24h.toLocaleString(), { exact: true })).toBeVisible();
  });

  // Charts / breakdowns
  test('displays role distribution section', async ({ adminPage: page }) => {
    await expect(page.locator('text=User Distribution by Role')).toBeVisible();
    await expect(page.locator('text=Regular Users')).toBeVisible();
    await expect(page.locator('text=Merchants')).toBeVisible();
    await expect(page.locator('text=Admins')).toBeVisible();
  });

  test('displays transaction status overview section', async ({ adminPage: page }) => {
    await expect(page.locator('text=Transaction Status Overview')).toBeVisible();
    await expect(page.getByText('Completed', { exact: true })).toBeVisible();
    await expect(page.getByText('Pending', { exact: true })).toBeVisible();
    await expect(page.getByText('Failed', { exact: true })).toBeVisible();
  });
});
