import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';

import { server } from '../test/msw-server';
import { createMockTransfer } from '../test/factories';
import { useTransferHistory } from './useTransfer';

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

describe('useTransferHistory', () => {
  it('fetches transfer history for a user', async () => {
    const { result } = renderHook(() => useTransferHistory('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.content).toHaveLength(2);
    expect(result.current.data?.content[0].amount).toBe(100);
  });

  it('does not fetch when userId is undefined', () => {
    const { result } = renderHook(() => useTransferHistory(undefined), {
      wrapper: createWrapper(),
    });

    expect(result.current.isFetching).toBe(false);
  });

  it('handles empty transfer list', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        HttpResponse.json({
          content: [],
          page: {
            totalElements: 0,
            totalPages: 0,
            size: 5,
            number: 0,
          },
        }),
      ),
    );

    const { result } = renderHook(() => useTransferHistory('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content).toHaveLength(0);
  });

  it('handles API errors', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        HttpResponse.json({ message: 'Error' }, { status: 500 }),
      ),
    );

    const { result } = renderHook(() => useTransferHistory('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it('returns transfers with correct statuses', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        HttpResponse.json({
          content: [
            createMockTransfer({ status: 'COMPLETED' }),
            createMockTransfer({ id: 't2', status: 'PROCESSING' }),
            createMockTransfer({ id: 't3', status: 'FAILED', failureReason: 'Insufficient funds' }),
          ],
          page: {
            totalElements: 3,
            totalPages: 1,
            size: 5,
            number: 0,
          },
        }),
      ),
    );

    const { result } = renderHook(() => useTransferHistory('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const statuses = result.current.data?.content.map((t) => t.status);
    expect(statuses).toEqual(['COMPLETED', 'PROCESSING', 'FAILED']);
  });
});
