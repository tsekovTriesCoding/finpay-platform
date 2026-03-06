import { test, expect, ADMIN_USER } from './admin-fixtures';

test.describe('Admin Navigation & Layout', () => {
  test('admin route redirects to login when unauthenticated', async ({ page }) => {
    // Navigate without admin auth setup (use raw page, not adminPage)
    await page.goto('/admin');
    await expect(page).toHaveURL(/.*\/login/);
  });

  test('non-admin user is redirected to /unauthorized', async ({ page }) => {
    // Set up as a regular USER
    const regularUser = { ...ADMIN_USER, role: 'USER' };
    await page.addInitScript((user) => {
      window.localStorage.setItem('user', JSON.stringify(user));
    }, regularUser);
    await page.route('http://localhost:8080/api/v1/auth/me', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(regularUser) }),
    );

    await page.goto('/admin');
    await expect(page).toHaveURL(/.*\/unauthorized/);
  });

  test('admin layout renders sidebar with all nav items', async ({ adminPage: page }) => {
    await page.goto('/admin');
    await expect(page.locator('text=FinPay Admin')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'User Management' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Transactions' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Wallets' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Audit Logs' })).toBeVisible();
    await expect(page.locator('text=System Overview')).toBeVisible();
    await expect(page.locator('text=Notifications')).toBeVisible();
  });

  test('sidebar shows admin email', async ({ adminPage: page }) => {
    await page.goto('/admin');
    await expect(page.locator(`text=${ADMIN_USER.email}`)).toBeVisible();
  });

  test('sidebar has Back to App button', async ({ adminPage: page }) => {
    await page.goto('/admin');
    const backBtn = page.locator('button:has-text("Back to App")');
    await expect(backBtn).toBeVisible();
  });

  test('sidebar has Logout button', async ({ adminPage: page }) => {
    await page.goto('/admin');
    const logoutBtn = page.locator('button:has-text("Logout")');
    await expect(logoutBtn).toBeVisible();
  });

  test('navigates between admin pages via sidebar links', async ({ adminPage: page }) => {
    await page.goto('/admin');

    // Click User Management
    await page.click('nav >> text=User Management');
    await expect(page).toHaveURL(/.*\/admin\/users/);
    await expect(page.locator('h1:has-text("User Management")')).toBeVisible();

    // Click Transactions
    await page.click('nav >> text=Transactions');
    await expect(page).toHaveURL(/.*\/admin\/transactions/);
    await expect(page.locator('h1:has-text("Transaction Monitor")')).toBeVisible();

    // Click Wallets
    await page.click('nav >> text=Wallets');
    await expect(page).toHaveURL(/.*\/admin\/wallets/);
    await expect(page.locator('h1:has-text("Wallet Management")')).toBeVisible();

    // Click Audit Logs
    await page.click('nav >> text=Audit Logs');
    await expect(page).toHaveURL(/.*\/admin\/audit-logs/);
    await expect(page.locator('h1:has-text("Audit Logs")')).toBeVisible();

    // Click back to Dashboard
    await page.click('nav >> text=Dashboard');
    await expect(page).toHaveURL(/.*\/admin$/);
    await expect(page.locator('h1:has-text("Admin Dashboard")')).toBeVisible();
  });

  test('active nav link is visually highlighted', async ({ adminPage: page }) => {
    await page.goto('/admin/users');

    // The "User Management" link should have the active class
    const activeLink = page.locator('nav a.bg-primary-600\\/20:has-text("User Management")');
    await expect(activeLink).toBeVisible();
  });

  test('direct URL access to admin sub-pages works', async ({ adminPage: page }) => {
    await page.goto('/admin/wallets');
    await expect(page.locator('h1:has-text("Wallet Management")')).toBeVisible();
    await expect(page.locator('text=FinPay Admin')).toBeVisible();
  });
});
