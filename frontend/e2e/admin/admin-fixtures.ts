import { test as base, type Page } from '@playwright/test';

// Admin user stored in localStorage for auth context
export const ADMIN_USER = {
  id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  email: 'admin@finpay.com',
  firstName: 'Platform',
  lastName: 'Admin',
  phoneNumber: null,
  status: 'ACTIVE' as const,
  role: 'ADMIN' as const,
  profileImageUrl: null,
  address: null,
  city: null,
  country: null,
  postalCode: null,
  emailVerified: true,
  phoneVerified: false,
  plan: 'ENTERPRISE',
  createdAt: '2024-01-15T10:00:00Z',
  updatedAt: '2025-06-01T12:00:00Z',
  lastLoginAt: '2025-06-14T09:30:00Z',
};

// Mock data factories

export function makeUser(overrides: Record<string, unknown> = {}) {
  return {
    id: crypto.randomUUID(),
    email: 'user@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phoneNumber: null,
    status: 'ACTIVE',
    role: 'USER',
    profileImageUrl: null,
    address: null,
    city: null,
    country: null,
    postalCode: null,
    emailVerified: true,
    phoneVerified: false,
    plan: 'STARTER',
    createdAt: '2025-03-10T08:00:00Z',
    updatedAt: '2025-06-01T12:00:00Z',
    lastLoginAt: '2025-06-13T14:00:00Z',
    ...overrides,
  };
}

export function makeTransaction(overrides: Record<string, unknown> = {}) {
  return {
    id: crypto.randomUUID(),
    type: 'TRANSFER',
    transactionReference: `TXN-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`,
    fromUserId: crypto.randomUUID(),
    toUserId: crypto.randomUUID(),
    amount: 150.0,
    currency: 'USD',
    status: 'COMPLETED',
    description: 'Test transfer',
    flagged: false,
    flagReason: null,
    createdAt: '2025-06-10T10:00:00Z',
    updatedAt: '2025-06-10T10:00:00Z',
    ...overrides,
  };
}

export function makeWallet(overrides: Record<string, unknown> = {}) {
  return {
    id: crypto.randomUUID(),
    userId: crypto.randomUUID(),
    balance: 1250.0,
    reservedBalance: 50.0,
    availableBalance: 1200.0,
    currency: 'USD',
    status: 'ACTIVE',
    plan: 'STARTER',
    dailyTransactionLimit: 5000,
    monthlyTransactionLimit: 50000,
    dailySpent: 300,
    monthlySpent: 5000,
    remainingDailyLimit: 4700,
    remainingMonthlyLimit: 45000,
    maxVirtualCards: null,
    multiCurrencyEnabled: null,
    apiAccessEnabled: null,
    createdAt: '2025-02-20T10:00:00Z',
    updatedAt: '2025-06-01T12:00:00Z',
    ...overrides,
  };
}

export function makeAuditLog(overrides: Record<string, unknown> = {}) {
  return {
    id: crypto.randomUUID(),
    actorId: ADMIN_USER.id,
    actorEmail: ADMIN_USER.email,
    action: 'USER_SUSPENDED',
    targetType: 'USER',
    targetId: crypto.randomUUID(),
    description: 'Suspended user for policy violation',
    previousState: null,
    newState: null,
    ipAddress: '192.168.1.100',
    serviceSource: 'user-service',
    createdAt: '2025-06-14T09:00:00Z',
    ...overrides,
  };
}

export function makePage<T>(content: T[], totalElements?: number) {
  const total = totalElements ?? content.length;
  return {
    content,
    page: {
      totalElements: total,
      totalPages: Math.max(1, Math.ceil(total / 20)),
      number: 0,
      size: 20,
    },
  };
}

export const DASHBOARD_METRICS = {
  totalUsers: 1250,
  activeUsers: 980,
  suspendedUsers: 45,
  pendingVerification: 120,
  adminCount: 5,
  merchantCount: 85,
  regularUserCount: 1160,
  recentAuditActions24h: 34,
  recentAuditActions7d: 210,
};

export const TRANSACTION_METRICS = {
  totalTransfers: 8500,
  completedTransfers: 7200,
  failedTransfers: 300,
  pendingTransfers: 150,
  totalTransferVolume: 2450000,
  totalBillPayments: 3200,
  completedBillPayments: 2900,
  failedBillPayments: 100,
  totalBillPaymentVolume: 890000,
  totalMoneyRequests: 1500,
  pendingMoneyRequests: 250,
  completedMoneyRequests: 1100,
  declinedMoneyRequests: 150,
  flaggedTransactions: 12,
};

export const WALLET_METRICS = {
  totalWallets: 1180,
  activeWallets: 1100,
  frozenWallets: 30,
  closedWallets: 50,
  totalBalance: 3750000,
};

// The gateway URL used by the axios instance
const API_BASE = 'http://localhost:8080';

/**
 * Sets up admin authentication by injecting localStorage and mocking the /me endpoint.
 * Also installs default API route mocks so admin pages don't hit real backends.
 */
export async function setupAdminAuth(page: Page) {
  // Seed localStorage before navigation
  await page.addInitScript((user) => {
    window.localStorage.setItem('user', JSON.stringify(user));
  }, ADMIN_USER);

  // Mock /me endpoint to return the admin user
  await page.route(`${API_BASE}/api/v1/auth/me`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ADMIN_USER) }),
  );
}

/**
 * Installs mock routes for ALL admin API endpoints with sensible defaults.
 * Call this once per test; individual tests can override specific routes afterward.
 */
export async function mockAdminAPIs(page: Page, overrides: {
  users?: unknown[];
  transactions?: unknown[];
  wallets?: unknown[];
  auditLogs?: unknown[];
  dashboardMetrics?: Record<string, unknown>;
  transactionMetrics?: Record<string, unknown>;
  walletMetrics?: Record<string, unknown>;
} = {}) {
  const users = overrides.users ?? [
    makeUser({ email: 'alice@example.com', firstName: 'Alice', lastName: 'Smith', role: 'USER', plan: 'PRO' }),
    makeUser({ email: 'bob@merchant.com', firstName: 'Bob', lastName: 'Jones', role: 'MERCHANT', status: 'ACTIVE' }),
    makeUser({ email: 'eve@suspended.com', firstName: 'Eve', lastName: 'Black', status: 'SUSPENDED', role: 'USER' }),
  ];

  const transactions = overrides.transactions ?? [
    makeTransaction({ amount: 500, status: 'COMPLETED', description: 'Monthly rent' }),
    makeTransaction({ amount: 75.50, status: 'PENDING', description: 'Utility bill' }),
    makeTransaction({ amount: 1200, status: 'FAILED', description: 'Large transfer' }),
  ];

  const wallets = overrides.wallets ?? [
    makeWallet({ balance: 5000, availableBalance: 4800, status: 'ACTIVE' }),
    makeWallet({ balance: 2500, availableBalance: 2500, status: 'FROZEN' }),
    makeWallet({ balance: 0, availableBalance: 0, status: 'CLOSED' }),
  ];

  const auditLogs = overrides.auditLogs ?? [
    makeAuditLog({ action: 'USER_SUSPENDED', description: 'Suspended user for TOS violation' }),
    makeAuditLog({ action: 'USER_ROLE_CHANGED', description: 'Changed role from USER to ADMIN', targetType: 'USER' }),
    makeAuditLog({ action: 'WALLET_FROZEN', description: 'Froze wallet for suspicious activity', targetType: 'WALLET' }),
  ];

  // User endpoints
  await page.route(`${API_BASE}/api/v1/admin/users?*`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(makePage(users)) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/users/dashboard/metrics`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(overrides.dashboardMetrics ?? DASHBOARD_METRICS) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/users/audit-logs?*`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(makePage(auditLogs)) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/users/audit-logs/search*`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(makePage(auditLogs)) }),
  );

  // Transaction endpoints
  await page.route(`${API_BASE}/api/v1/admin/transactions?*`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(makePage(transactions)) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/transactions/metrics`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(overrides.transactionMetrics ?? TRANSACTION_METRICS) }),
  );

  // Wallet endpoints
  await page.route(`${API_BASE}/api/v1/admin/wallets?*`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(makePage(wallets)) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/wallets/metrics`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(overrides.walletMetrics ?? WALLET_METRICS) }),
  );

  // Mutation endpoints (suspend, unsuspend, role change, freeze, unfreeze, force-password-reset)
  await page.route(`${API_BASE}/api/v1/admin/users/*/suspend`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(users[0]) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/users/*/unsuspend`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(users[0]) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/users/*/role`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(users[0]) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/users/*/force-password-reset`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(users[0]) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/wallets/user/*/freeze`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(wallets[0]) }),
  );
  await page.route(`${API_BASE}/api/v1/admin/wallets/user/*/unfreeze`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(wallets[0]) }),
  );
}

/** Extended Playwright test with admin auth pre-configured */
export const test = base.extend<{ adminPage: Page }>({
  adminPage: async ({ page }, apply) => {
    await setupAdminAuth(page);
    await mockAdminAPIs(page);
    await apply(page);
  },
});

export { expect } from '@playwright/test';
