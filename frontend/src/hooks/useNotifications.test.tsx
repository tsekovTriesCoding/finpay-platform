import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';

import { server } from '../test/msw-server';
import { createMockNotification } from '../test/factories';
import { useNotifications, useUnreadCount } from './useNotifications';

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

describe('useNotifications', () => {
  it('fetches notifications for a user', async () => {
    const { result } = renderHook(() => useNotifications('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(2);
    expect(result.current.data?.[0].subject).toBe('Payment Completed');
  });

  it('handles empty notifications', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/notifications/user/:userId`, () =>
        HttpResponse.json([]),
      ),
    );

    const { result } = renderHook(() => useNotifications('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(0);
  });
});

describe('useUnreadCount', () => {
  it('fetches unread notification count', async () => {
    const { result } = renderHook(() => useUnreadCount('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe(3);
  });

  it('handles zero unread count', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/notifications/user/:userId/unread/count`, () =>
        HttpResponse.json({ count: 0 }),
      ),
    );

    const { result } = renderHook(() => useUnreadCount('user-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe(0);
  });
});
