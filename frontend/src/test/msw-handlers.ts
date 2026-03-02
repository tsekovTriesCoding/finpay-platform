import { http, HttpResponse } from 'msw';
import {
  createMockUser,
  createMockAuthResponse,
  createMockWallet,
  createMockTransfer,
  createMockBillPayment,
  createMockNotification,
  createMockNotificationPreferences,
  createMockUserSearchResult,
} from './factories';

const BASE_URL = 'http://localhost:8080';

/** Default MSW handlers that return successful responses. Override per-test as needed. */
export const handlers = [
  // Auth
  http.post(`${BASE_URL}/api/v1/auth/login`, () =>
    HttpResponse.json(createMockAuthResponse()),
  ),

  http.post(`${BASE_URL}/api/v1/auth/register`, () =>
    HttpResponse.json(createMockAuthResponse()),
  ),

  http.post(`${BASE_URL}/api/v1/auth/refresh`, () =>
    HttpResponse.json(createMockAuthResponse()),
  ),

  http.post(`${BASE_URL}/api/v1/auth/logout`, () =>
    new HttpResponse(null, { status: 204 }),
  ),

  http.post(`${BASE_URL}/api/v1/auth/logout-all`, () =>
    new HttpResponse(null, { status: 204 }),
  ),

  http.get(`${BASE_URL}/api/v1/auth/me`, () =>
    HttpResponse.json(createMockUser()),
  ),

  http.put(`${BASE_URL}/api/v1/auth/plan`, () =>
    HttpResponse.json({
      userId: 'user-1',
      previousPlan: 'STARTER',
      newPlan: 'PRO',
      message: 'Plan upgraded successfully',
    }),
  ),

  // User
  // IMPORTANT: /search must come before /:userId so the parameterised route doesn't swallow it
  http.get(`${BASE_URL}/api/v1/users/search`, () =>
    HttpResponse.json({
      content: [
        createMockUserSearchResult(),
        createMockUserSearchResult({ id: 'user-3', email: 'bob@example.com', firstName: 'Bob', lastName: 'Jones' }),
      ],
      totalElements: 2,
      totalPages: 1,
      size: 5,
      number: 0,
    }),
  ),

  http.get(`${BASE_URL}/api/v1/users/:userId`, () =>
    HttpResponse.json(createMockUser()),
  ),

  http.put(`${BASE_URL}/api/v1/users/:userId`, () =>
    HttpResponse.json(createMockUser()),
  ),

  // Wallet
  http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
    HttpResponse.json(createMockWallet()),
  ),

  http.post(`${BASE_URL}/api/v1/wallets/deposit`, () =>
    HttpResponse.json(createMockWallet({ balance: 6000, availableBalance: 5800 })),
  ),

  // Transfers
  http.post(`${BASE_URL}/api/v1/payments/transfers`, () =>
    HttpResponse.json(createMockTransfer({ status: 'PROCESSING' })),
  ),

  http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
    HttpResponse.json({
      content: [
        createMockTransfer(),
        createMockTransfer({
          id: 'transfer-2',
          transactionReference: 'TXN-002',
          amount: 250,
          status: 'PROCESSING',
        }),
      ],
      totalElements: 2,
      totalPages: 1,
      size: 5,
      number: 0,
    }),
  ),

  http.get(`${BASE_URL}/api/v1/payments/transfers/:transferId`, () =>
    HttpResponse.json(createMockTransfer()),
  ),

  // Money Requests
  http.post(`${BASE_URL}/api/v1/payments/requests`, () =>
    HttpResponse.json(
      createMockTransfer({ status: 'PROCESSING', transferType: 'REQUEST_PAYMENT' }),
    ),
  ),

  http.get(`${BASE_URL}/api/v1/payments/requests/user/:userId`, () =>
    HttpResponse.json({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    }),
  ),

  http.get(`${BASE_URL}/api/v1/payments/requests/pending/incoming`, () =>
    HttpResponse.json({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    }),
  ),

  http.get(`${BASE_URL}/api/v1/payments/requests/pending/outgoing`, () =>
    HttpResponse.json({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    }),
  ),

  http.get(`${BASE_URL}/api/v1/payments/requests/pending/count`, () =>
    HttpResponse.json(0),
  ),

  http.post(`${BASE_URL}/api/v1/payments/requests/:requestId/approve`, () =>
    HttpResponse.json(createMockTransfer()),
  ),

  http.post(`${BASE_URL}/api/v1/payments/requests/:requestId/decline`, () =>
    HttpResponse.json(createMockTransfer()),
  ),

  http.post(`${BASE_URL}/api/v1/payments/requests/:requestId/cancel`, () =>
    HttpResponse.json(createMockTransfer()),
  ),

  // ── Bill Payments ─────────────────────────────────────────────────────
  http.post(`${BASE_URL}/api/v1/payments/bills`, () =>
    HttpResponse.json(createMockBillPayment({ status: 'PROCESSING' })),
  ),

  http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, () =>
    HttpResponse.json({
      content: [createMockBillPayment()],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 10,
    }),
  ),

  http.get(`${BASE_URL}/api/v1/payments/bills/categories`, () =>
    HttpResponse.json(['ELECTRICITY', 'WATER', 'INTERNET', 'PHONE']),
  ),

  // Notifications
  http.get(`${BASE_URL}/api/v1/notifications/user/:userId`, () =>
    HttpResponse.json([
      createMockNotification(),
      createMockNotification({
        id: 'notif-2',
        type: 'TRANSFER_SENT',
        subject: 'Transfer Sent',
        content: 'You sent $50.00 to Jane.',
        readAt: '2025-01-15T13:00:00Z',
        status: 'READ',
      }),
    ]),
  ),

  http.get(`${BASE_URL}/api/v1/notifications/user/:userId/unread`, () =>
    HttpResponse.json([createMockNotification()]),
  ),

  http.get(`${BASE_URL}/api/v1/notifications/user/:userId/unread/count`, () =>
    HttpResponse.json({ count: 3 }),
  ),

  http.post(`${BASE_URL}/api/v1/notifications/:notificationId/read`, () =>
    HttpResponse.json(createMockNotification({ readAt: new Date().toISOString(), status: 'READ' })),
  ),

  http.post(`${BASE_URL}/api/v1/notifications/user/:userId/read-all`, () =>
    new HttpResponse(null, { status: 204 }),
  ),

  http.delete(`${BASE_URL}/api/v1/notifications/:notificationId`, () =>
    new HttpResponse(null, { status: 204 }),
  ),

  http.get(`${BASE_URL}/api/v1/notifications/user/:userId/preferences`, () =>
    HttpResponse.json(createMockNotificationPreferences()),
  ),

  http.put(`${BASE_URL}/api/v1/notifications/user/:userId/preferences`, () =>
    HttpResponse.json(createMockNotificationPreferences()),
  ),

  // Transaction Detail
  http.get(`${BASE_URL}/api/v1/transactions/transfers/:id`, () =>
    HttpResponse.json({
      id: 'transfer-1',
      transactionReference: 'TXN-001',
      type: 'TRANSFER',
      senderUserId: 'user-1',
      recipientUserId: 'user-2',
      amount: 100,
      currency: 'USD',
      processingFee: 0,
      totalAmount: 100,
      status: 'COMPLETED',
      failureReason: null,
      title: 'Transfer to Jane',
      subtitle: 'Completed',
      description: 'Test transfer',
      metadata: {},
      timeline: [],
      availableActions: [],
      createdAt: '2025-01-15T11:55:00Z',
      completedAt: '2025-01-15T12:00:00Z',
      updatedAt: '2025-01-15T12:00:00Z',
    }),
  ),

  http.get(`${BASE_URL}/api/v1/transactions/bills/:id`, () =>
    HttpResponse.json({
      id: 'bill-1',
      transactionReference: 'BILL-001',
      type: 'BILL_PAYMENT',
      senderUserId: 'user-1',
      recipientUserId: null,
      amount: 150,
      currency: 'USD',
      processingFee: 2.5,
      totalAmount: 152.5,
      status: 'COMPLETED',
      failureReason: null,
      title: 'Electricity Bill',
      subtitle: 'City Power Co.',
      description: null,
      metadata: {},
      timeline: [],
      availableActions: [],
      createdAt: '2025-01-15T11:55:00Z',
      completedAt: '2025-01-15T12:00:00Z',
      updatedAt: '2025-01-15T12:00:00Z',
    }),
  ),
];
