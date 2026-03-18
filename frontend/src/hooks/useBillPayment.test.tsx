import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';

import { server } from '../test/msw-server';
import { createMockBillPayment } from '../test/factories';
import { useBillPayments } from './useBillPayment';

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

describe('useBillPayments', () => {
  it('fetches bill payments for a user', async () => {
    const { result } = renderHook(() => useBillPayments('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.content).toHaveLength(1);
    expect(result.current.data?.content[0].billerName).toBe('City Power Co.');
  });

  it('does not fetch when userId is undefined', () => {
    const { result } = renderHook(() => useBillPayments(undefined), {
      wrapper: createWrapper(),
    });

    expect(result.current.isFetching).toBe(false);
  });

  it('handles API errors', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, () =>
        HttpResponse.json({ message: 'Error' }, { status: 500 }),
      ),
    );

    const { result } = renderHook(() => useBillPayments('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it('returns bill payments with various categories', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, () =>
        HttpResponse.json({
          content: [
            createMockBillPayment({ category: 'ELECTRICITY' }),
            createMockBillPayment({ id: 'b2', category: 'WATER', billerName: 'Metro Water' }),
            createMockBillPayment({ id: 'b3', category: 'INTERNET', billerName: 'FiberNet ISP' }),
          ],
          page: {
            totalElements: 3,
            totalPages: 1,
            number: 0,
            size: 10,
          },
        }),
      ),
    );

    const { result } = renderHook(() => useBillPayments('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const categories = result.current.data?.content.map((b) => b.category);
    expect(categories).toEqual(['ELECTRICITY', 'WATER', 'INTERNET']);
  });

  it('supports pagination parameters', async () => {
    let capturedPage: string | null = null;
    let capturedSize: string | null = null;

    server.use(
      http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, ({ request }) => {
        const url = new URL(request.url);
        capturedPage = url.searchParams.get('page');
        capturedSize = url.searchParams.get('size');
        return HttpResponse.json({
          content: [],
          page: {
            totalElements: 0,
            totalPages: 0,
            number: 2,
            size: 20,
          },
        });
      }),
    );

    const { result } = renderHook(() => useBillPayments('user-1', 2, 20), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(capturedPage).toBe('2');
    expect(capturedSize).toBe('20');
  });
});
