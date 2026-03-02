import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { http, HttpResponse } from 'msw';

import { server } from '../test/msw-server';
import { paymentService } from './paymentApi';

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('paymentService', () => {
  describe('searchUsers', () => {
    it('returns matching users', async () => {
      const result = await paymentService.searchUsers('jane', 'user-1');

      expect(result.content).toHaveLength(2);
      expect(result.content[0].firstName).toBe('Jane');
    });

    it('handles empty results', async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/users/search`, () =>
          HttpResponse.json({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 5,
            number: 0,
          }),
        ),
      );

      const result = await paymentService.searchUsers('nobody', 'user-1');
      expect(result.content).toHaveLength(0);
    });
  });

  describe('sendMoney', () => {
    it('sends a money transfer', async () => {
      const transfer = await paymentService.sendMoney('user-1', {
        recipientUserId: 'user-2',
        amount: 100,
        currency: 'USD',
        description: 'Test',
      });

      expect(transfer.status).toBe('PROCESSING');
      expect(transfer.amount).toBe(100);
    });
  });

  describe('getTransferHistory', () => {
    it('returns paginated transfer history', async () => {
      const history = await paymentService.getTransferHistory('user-1');

      expect(history.content).toHaveLength(2);
      expect(history.totalElements).toBe(2);
    });
  });

  describe('getTransfer', () => {
    it('fetches a single transfer', async () => {
      const transfer = await paymentService.getTransfer('transfer-1');

      expect(transfer.transactionReference).toBe('TXN-001');
      expect(transfer.status).toBe('COMPLETED');
    });
  });

  describe('money requests', () => {
    it('creates a money request', async () => {
      const result = await paymentService.createMoneyRequest('user-1', {
        payerUserId: 'user-2',
        amount: 50,
        currency: 'USD',
      });

      expect(result).toBeDefined();
    });

    it('gets pending incoming requests', async () => {
      const result = await paymentService.getPendingIncomingRequests('user-1');
      expect(result.content).toHaveLength(0);
    });

    it('gets pending request count', async () => {
      const count = await paymentService.getPendingRequestCount('user-1');
      expect(count).toBe(0);
    });
  });
});
