import type { User, AuthResponse, AccountPlan } from '../api/authApi';
import type { Wallet } from '../api/walletApi';
import type { MoneyTransfer, MoneyRequest, UserSearchResult } from '../api/paymentApi';
import type { BillPayment } from '../api/billPaymentApi';
import type { Notification, NotificationPreferences } from '../api/notificationApi';

// User

export function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: 'user-1',
    email: 'john@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phoneNumber: '+1234567890',
    status: 'ACTIVE',
    role: 'USER',
    profileImageUrl: null,
    address: '123 Main St',
    city: 'New York',
    country: 'US',
    postalCode: '10001',
    emailVerified: true,
    phoneVerified: false,
    plan: 'STARTER',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-15T00:00:00Z',
    lastLoginAt: '2025-01-20T00:00:00Z',
    ...overrides,
  };
}

export function createMockAuthResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: createMockUser(),
    ...overrides,
  };
}

// Wallet

export function createMockWallet(overrides: Partial<Wallet> = {}): Wallet {
  return {
    id: 'wallet-1',
    userId: 'user-1',
    balance: 5000.0,
    reservedBalance: 200.0,
    availableBalance: 4800.0,
    currency: 'USD',
    status: 'ACTIVE',
    plan: 'STARTER',
    dailyTransactionLimit: 5000,
    monthlyTransactionLimit: 50000,
    dailySpent: 500,
    monthlySpent: 2000,
    remainingDailyLimit: 4500,
    remainingMonthlyLimit: 48000,
    maxVirtualCards: 1,
    multiCurrencyEnabled: false,
    apiAccessEnabled: false,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-15T00:00:00Z',
    ...overrides,
  };
}

// Transfers

export function createMockTransfer(overrides: Partial<MoneyTransfer> = {}): MoneyTransfer {
  return {
    id: 'transfer-1',
    transactionReference: 'TXN-001',
    senderUserId: 'user-1',
    recipientUserId: 'user-2',
    amount: 100.0,
    currency: 'USD',
    description: 'Test transfer',
    transferType: 'SEND',
    status: 'COMPLETED',
    sourceRequestId: null,
    failureReason: null,
    completedAt: '2025-01-15T12:00:00Z',
    createdAt: '2025-01-15T11:55:00Z',
    ...overrides,
  };
}

// Money Requests

export function createMockMoneyRequest(overrides: Partial<MoneyRequest> = {}): MoneyRequest {
  return {
    id: 'request-1',
    requestReference: 'REQ-001',
    requesterUserId: 'user-1',
    payerUserId: 'user-2',
    amount: 50.0,
    currency: 'USD',
    description: 'Test request',
    status: 'PENDING_APPROVAL',
    sagaStatus: null,
    failureReason: null,
    approvedAt: null,
    declinedAt: null,
    completedAt: null,
    expiresAt: '2025-02-15T00:00:00Z',
    createdAt: '2025-01-15T00:00:00Z',
    ...overrides,
  };
}

// Bill Payments

export function createMockBillPayment(overrides: Partial<BillPayment> = {}): BillPayment {
  return {
    id: 'bill-1',
    userId: 'user-1',
    transactionReference: 'BILL-001',
    category: 'ELECTRICITY',
    billerName: 'City Power Co.',
    billerCode: 'ELEC-001',
    accountNumber: '1234567890',
    accountHolderName: 'John Doe',
    amount: 150.0,
    currency: 'USD',
    processingFee: 2.5,
    totalAmount: 152.5,
    status: 'COMPLETED',
    description: 'Monthly electricity',
    failureReason: null,
    billerReference: 'BP-REF-001',
    processedAt: '2025-01-15T12:00:00Z',
    createdAt: '2025-01-15T11:55:00Z',
    updatedAt: '2025-01-15T12:00:00Z',
    ...overrides,
  };
}

// Notifications

export function createMockNotification(overrides: Partial<Notification> = {}): Notification {
  return {
    id: 'notif-1',
    userId: 'user-1',
    type: 'PAYMENT_COMPLETED',
    channel: 'IN_APP',
    subject: 'Payment Completed',
    content: 'Your payment of $100.00 has been completed.',
    recipient: null,
    status: 'DELIVERED',
    errorMessage: null,
    sentAt: '2025-01-15T12:00:00Z',
    readAt: null,
    createdAt: '2025-01-15T12:00:00Z',
    ...overrides,
  };
}

export function createMockNotificationPreferences(
  overrides: Partial<NotificationPreferences> = {},
): NotificationPreferences {
  return {
    id: 'pref-1',
    userId: 'user-1',
    emailEnabled: true,
    smsEnabled: false,
    pushEnabled: true,
    inAppEnabled: true,
    paymentNotifications: true,
    securityNotifications: true,
    promotionalNotifications: false,
    systemNotifications: true,
    ...overrides,
  };
}

// User Search

export function createMockUserSearchResult(overrides: Partial<UserSearchResult> = {}): UserSearchResult {
  return {
    id: 'user-2',
    email: 'jane@example.com',
    firstName: 'Jane',
    lastName: 'Smith',
    profileImageUrl: null,
    ...overrides,
  };
}

// Plan helpers

export const MOCK_PLANS: AccountPlan[] = ['STARTER', 'PRO', 'ENTERPRISE'];
