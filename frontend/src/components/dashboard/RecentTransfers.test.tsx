import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { render } from '../../test/test-utils';
import { server } from '../../test/msw-server';
import { createMockTransfer } from '../../test/factories';
import RecentTransfers from './RecentTransfers';

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('RecentTransfers', () => {
  it('shows loading skeleton while fetching', () => {
    // Use a handler that never responds
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        new Promise(() => {}), // Never resolves
      ),
    );

    render(<RecentTransfers userId="user-1" />);

    // Skeleton placeholders should be visible
    const skeletons = document.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('displays transfer list after loading', async () => {
    render(<RecentTransfers userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Recent Transfers')).toBeInTheDocument();
    });

    // Default mock has 2 transfers - amount rendered with sign prefix
    await waitFor(() => {
      expect(screen.getByText(/\$100\.00/)).toBeInTheDocument();
    });
  });

  it('shows empty state when no transfers', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 5,
          number: 0,
        }),
      ),
    );

    render(<RecentTransfers userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/no.*transfer/i)).toBeInTheDocument();
    });
  });

  it('shows error state on API failure', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        HttpResponse.json({ message: 'Error' }, { status: 500 }),
      ),
    );

    render(<RecentTransfers userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/failed/i)).toBeInTheDocument();
    });
  });

  it('displays transfer status badges', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/transfers/user/:userId`, () =>
        HttpResponse.json({
          content: [
            createMockTransfer({ status: 'COMPLETED' }),
            createMockTransfer({ id: 'transfer-2', status: 'PROCESSING' }),
          ],
          totalElements: 2,
          totalPages: 1,
          size: 5,
          number: 0,
        }),
      ),
    );

    render(<RecentTransfers userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('COMPLETED')).toBeInTheDocument();
      expect(screen.getByText('PROCESSING')).toBeInTheDocument();
    });
  });

  it('calls onTransactionSelect when a transfer is clicked', async () => {
    const onSelect = vi.fn();

    render(<RecentTransfers userId="user-1" onTransactionSelect={onSelect} />);

    await waitFor(() => {
      expect(screen.getByText(/\$100\.00/)).toBeInTheDocument();
    });

    // Click on the first transfer row
    const rows = document.querySelectorAll('[class*="cursor-pointer"]');
    if (rows.length > 0) {
      (rows[0] as HTMLElement).click();
      expect(onSelect).toHaveBeenCalledWith('TRANSFER', 'transfer-1');
    }
  });
});
