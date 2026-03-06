import { test, expect, makeUser, makePage } from './admin-fixtures';

const API_BASE = 'http://localhost:8080';

test.describe('User Management Page', () => {
  const activeUser = makeUser({ id: '00000000-0000-0000-0000-000000000001', email: 'alice@example.com', firstName: 'Alice', lastName: 'Smith', role: 'USER', plan: 'PRO' });
  const suspendedUser = makeUser({ id: '00000000-0000-0000-0000-000000000002', email: 'eve@suspended.com', firstName: 'Eve', lastName: 'Black', status: 'SUSPENDED', role: 'USER' });

  test.beforeEach(async ({ adminPage: page }) => {
    await page.goto('/admin/users');
    await expect(page.locator('h1:has-text("User Management")')).toBeVisible();
  });

  // Page structure
  test('displays page header and description', async ({ adminPage: page }) => {
    await expect(page.locator('text=Manage platform users, roles, and account status')).toBeVisible();
  });

  test('displays search input', async ({ adminPage: page }) => {
    const searchInput = page.locator('input[placeholder="Search by name or email..."]');
    await expect(searchInput).toBeVisible();
  });

  test('displays filter dropdowns', async ({ adminPage: page }) => {
    await expect(page.locator('select:has(option:has-text("All Statuses"))')).toBeVisible();
    await expect(page.locator('select:has(option:has-text("All Roles"))')).toBeVisible();
  });

  // Data table
  test('renders user data in the table', async ({ adminPage: page }) => {
    await expect(page.locator('text=alice@example.com')).toBeVisible();
    await expect(page.locator('text=bob@merchant.com')).toBeVisible();
    await expect(page.locator('text=eve@suspended.com')).toBeVisible();
  });

  test('displays correct column headers', async ({ adminPage: page }) => {
    await expect(page.locator('th:has-text("Email")')).toBeVisible();
    await expect(page.locator('th:has-text("Role")')).toBeVisible();
    await expect(page.locator('th:has-text("Status")')).toBeVisible();
    await expect(page.locator('th:has-text("Plan")')).toBeVisible();
    await expect(page.locator('th:has-text("Verified")')).toBeVisible();
    await expect(page.locator('th:has-text("Created")')).toBeVisible();
  });

  test('displays role badges for each user', async ({ adminPage: page }) => {
    await expect(page.locator('span:has-text("USER")').first()).toBeVisible();
    await expect(page.locator('span:has-text("MERCHANT")')).toBeVisible();
  });

  test('displays status badges correctly', async ({ adminPage: page }) => {
    await expect(page.locator('span:has-text("ACTIVE")').first()).toBeVisible();
    await expect(page.locator('span:has-text("SUSPENDED")')).toBeVisible();
  });

  test('each row has a View button', async ({ adminPage: page }) => {
    const viewButtons = page.locator('button:has-text("View")');
    await expect(viewButtons).toHaveCount(3);
  });

  // Export buttons
  test('displays CSV and PDF export buttons', async ({ adminPage: page }) => {
    await expect(page.locator('button:has-text("CSV")')).toBeVisible();
    await expect(page.locator('button:has-text("PDF")')).toBeVisible();
  });

  // Pagination info
  test('shows pagination info', async ({ adminPage: page }) => {
    await expect(page.locator('text=/Showing.*of.*results/')).toBeVisible();
  });

  // Filters
  test('status filter has correct options', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Statuses"))');
    await expect(select.locator('option:text-is("Active")')).toBeAttached();
    await expect(select.locator('option:text-is("Inactive")')).toBeAttached();
    await expect(select.locator('option:text-is("Suspended")')).toBeAttached();
    await expect(select.locator('option:text-is("Pending Verification")')).toBeAttached();
  });

  test('role filter has correct options', async ({ adminPage: page }) => {
    const select = page.locator('select:has(option:has-text("All Roles"))');
    await expect(select.locator('option:text-is("User")')).toBeAttached();
    await expect(select.locator('option:text-is("Admin")')).toBeAttached();
    await expect(select.locator('option:text-is("Merchant")')).toBeAttached();
  });

  test('search input triggers API call with search param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/users?*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([activeUser])),
      });
    });

    const searchInput = page.locator('input[placeholder="Search by name or email..."]');
    await searchInput.fill('alice');

    // Wait for debounced API call
    await page.waitForTimeout(500);
    expect(capturedUrl).toContain('search=alice');
  });

  test('status filter triggers API call with status param', async ({ adminPage: page }) => {
    let capturedUrl = '';
    await page.route(`${API_BASE}/api/v1/admin/users?*`, (route) => {
      capturedUrl = route.request().url();
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(makePage([suspendedUser])),
      });
    });

    await page.locator('select:has(option:has-text("All Statuses"))').selectOption('SUSPENDED');
    await page.waitForTimeout(300);
    expect(capturedUrl).toContain('status=SUSPENDED');
  });

  // User detail panel
  test('clicking View opens the slide-over panel', async ({ adminPage: page }) => {
    const firstViewBtn = page.locator('button:has-text("View")').first();
    await firstViewBtn.click();

    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel).toBeVisible();
  });

  test('user detail panel shows user info', async ({ adminPage: page }) => {
    const firstViewBtn = page.locator('button:has-text("View")').first();
    await firstViewBtn.click();

    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel.locator('text=alice@example.com')).toBeVisible();
    await expect(detailPanel.locator('text=Alice Smith')).toBeVisible();
  });

  test('user detail panel shows action buttons', async ({ adminPage: page }) => {
    await page.locator('button:has-text("View")').first().click();

    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel).toBeVisible();
    // Active user should see suspend option
    await expect(detailPanel.locator('text=Suspend User')).toBeVisible();
    await expect(detailPanel.locator('text=Force Password Reset')).toBeVisible();
  });

  test('user detail panel shows role change options', async ({ adminPage: page }) => {
    await page.locator('button:has-text("View")').first().click();

    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel).toBeVisible();
    // Active user with role USER should see Promote to Admin and Make Merchant buttons
    await expect(detailPanel.locator('text=Promote to Admin')).toBeVisible();
    await expect(detailPanel.locator('text=Make Merchant')).toBeVisible();
  });

  test('closing panel via X button works', async ({ adminPage: page }) => {
    await page.locator('button:has-text("View")').first().click();
    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel).toBeVisible();

    // Close via X
    await detailPanel.locator('button:has(svg.lucide-x)').click();
    await expect(detailPanel).not.toBeVisible();
  });

  // Confirm modal
  test('suspend action shows confirmation modal', async ({ adminPage: page }) => {
    await page.locator('button:has-text("View")').first().click();
    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel).toBeVisible();

    await detailPanel.locator('text=Suspend User').click();
    // ConfirmModal should appear
    await expect(page.locator('text=Suspend User').last()).toBeVisible();
    await expect(page.locator('text=/will be suspended/')).toBeVisible();
    await expect(page.locator('button:has-text("Confirm")')).toBeVisible();
    await expect(page.locator('button:has-text("Cancel")')).toBeVisible();
  });

  test('cancel on confirm modal dismisses it', async ({ adminPage: page }) => {
    await page.locator('button:has-text("View")').first().click();
    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await detailPanel.locator('text=Suspend User').click();
    await expect(page.locator('button:has-text("Confirm")')).toBeVisible();

    await page.locator('button:has-text("Cancel")').click();
    await expect(page.locator('button:has-text("Confirm")')).not.toBeVisible();
  });

  test('confirm suspend calls the API and closes modals', async ({ adminPage: page }) => {
    let suspensCalled = false;
    await page.route(`${API_BASE}/api/v1/admin/users/*/suspend`, (route) => {
      suspensCalled = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ...activeUser, status: 'SUSPENDED' }),
      });
    });

    await page.locator('button:has-text("View")').first().click();
    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await detailPanel.locator('text=Suspend User').click();
    await page.locator('button:has-text("Confirm")').click();

    // Wait for API call and modal close
    await page.waitForTimeout(500);
    expect(suspensCalled).toBe(true);
  });

  // Suspended user detail panel
  test('suspended user shows unsuspend action and warning', async ({ adminPage: page }) => {
    // Click View on the suspended user (eve@suspended.com)
    const eveRow = page.locator('tr:has-text("eve@suspended.com")');
    await eveRow.locator('button:has-text("View")').click();

    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await expect(detailPanel).toBeVisible();
    await expect(detailPanel.locator('text=Unsuspend User')).toBeVisible();
    await expect(detailPanel.locator('text=Account Suspended')).toBeVisible();
    await expect(detailPanel.locator('text=This user cannot access the platform.')).toBeVisible();
  });

  // Force password reset
  test('force password reset shows confirmation modal', async ({ adminPage: page }) => {
    await page.locator('button:has-text("View")').first().click();
    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await detailPanel.locator('text=Force Password Reset').click();

    await expect(page.locator('text=Force Password Reset').last()).toBeVisible();
    await expect(page.locator('text=/will be required to change their password/')).toBeVisible();
  });

  test('confirm force password reset calls the API', async ({ adminPage: page }) => {
    let resetCalled = false;
    await page.route(`${API_BASE}/api/v1/admin/users/*/force-password-reset`, (route) => {
      resetCalled = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(activeUser),
      });
    });

    await page.locator('button:has-text("View")').first().click();
    const detailPanel = page.locator('aside:has(h2:has-text("User Details"))');
    await detailPanel.locator('text=Force Password Reset').click();
    await page.locator('button:has-text("Confirm")').click();

    await page.waitForTimeout(500);
    expect(resetCalled).toBe(true);
  });
});
