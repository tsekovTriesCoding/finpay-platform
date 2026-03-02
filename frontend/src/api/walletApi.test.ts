import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { http, HttpResponse } from 'msw';

import { server } from '../test/msw-server';
import { walletService } from './walletApi';

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('walletService', () => {
  describe('getWallet', () => {
    it('fetches wallet by userId', async () => {
      const wallet = await walletService.getWallet('user-1');

      expect(wallet.userId).toBe('user-1');
      expect(wallet.balance).toBe(5000);
      expect(wallet.currency).toBe('USD');
    });

    it('throws on 404', async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
          HttpResponse.json({ message: 'Not found' }, { status: 404 }),
        ),
      );

      await expect(walletService.getWallet('nonexistent')).rejects.toThrow();
    });
  });

  describe('deposit', () => {
    it('deposits funds and returns updated wallet', async () => {
      const wallet = await walletService.deposit('user-1', 1000, 'USD');

      expect(wallet.balance).toBe(6000);
      expect(wallet.availableBalance).toBe(5800);
    });
  });
});
