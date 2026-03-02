import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';

import { server } from '../../test/msw-server';
import { NotificationContext } from '../../contexts/NotificationContext';
import NotificationBell from './NotificationBell';

import { render as rtlRender } from '@testing-library/react';

const BASE_URL = 'http://localhost:8080';

function renderWithNotificationContext(ui: ReactNode, { isConnected = false } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });

  return rtlRender(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NotificationContext.Provider value={{ isConnected }}>
          {ui}
        </NotificationContext.Provider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('NotificationBell', () => {
  it('shows unread count badge', async () => {
    renderWithNotificationContext(<NotificationBell userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  it('hides badge when no unread notifications', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/notifications/user/:userId/unread/count`, () =>
        HttpResponse.json({ count: 0 }),
      ),
    );

    renderWithNotificationContext(<NotificationBell userId="user-1" />);

    // Wait for query to settle, then confirm no badge
    await waitFor(() => {
      expect(screen.queryByText('0')).not.toBeInTheDocument();
    });
  });

  it('renders bell button with proper title', async () => {
    renderWithNotificationContext(<NotificationBell userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByTitle(/unread notifications/i)).toBeInTheDocument();
    });
  });

  it('shows 99+ when count exceeds 99', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/notifications/user/:userId/unread/count`, () =>
        HttpResponse.json({ count: 150 }),
      ),
    );

    renderWithNotificationContext(<NotificationBell userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('99+')).toBeInTheDocument();
    });
  });

  it('shows connection indicator', async () => {
    renderWithNotificationContext(<NotificationBell userId="user-1" />, {
      isConnected: true,
    });

    await waitFor(() => {
      expect(screen.getByTitle('Real-time connected')).toBeInTheDocument();
    });
  });

  it('shows disconnected indicator when not connected', async () => {
    renderWithNotificationContext(<NotificationBell userId="user-1" />, {
      isConnected: false,
    });

    await waitFor(() => {
      expect(screen.getByTitle('Connecting...')).toBeInTheDocument();
    });
  });
});
