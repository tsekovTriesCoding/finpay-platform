import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';

import { server } from '../test/msw-server';
import { createMockWallet } from '../test/factories';
import { useWallet } from './useWallet';

const BASE_URL = 'http://localhost:8080';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('useWallet', () => {
  it('fetches wallet data for a user', async () => {
    const { result } = renderHook(() => useWallet('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.balance).toBe(5000);
    expect(result.current.data?.availableBalance).toBe(4800);
    expect(result.current.data?.currency).toBe('USD');
  });

  it('does not fetch when userId is undefined', () => {
    const { result } = renderHook(() => useWallet(undefined), {
      wrapper: createWrapper(),
    });

    expect(result.current.isFetching).toBe(false);
    expect(result.current.data).toBeUndefined();
  });

  it('handles API errors', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
        HttpResponse.json({ message: 'Forbidden' }, { status: 403 }),
      ),
    );

    const { result } = renderHook(() => useWallet('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it('returns correct wallet fields', async () => {
    const wallet = createMockWallet({
      balance: 10000,
      availableBalance: 9500,
      reservedBalance: 500,
      plan: 'PRO',
    });

    server.use(
      http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
        HttpResponse.json(wallet),
      ),
    );

    const { result } = renderHook(() => useWallet('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toMatchObject({
      balance: 10000,
      availableBalance: 9500,
      reservedBalance: 500,
      plan: 'PRO',
    });
  });
});
