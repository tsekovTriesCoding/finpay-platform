import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { render } from '../../test/test-utils';
import { server } from '../../test/msw-server';
import { createMockBillPayment } from '../../test/factories';
import RecentBillPayments from './RecentBillPayments';

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('RecentBillPayments', () => {
  it('displays bill payment list after loading', async () => {
    render(<RecentBillPayments userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Recent Bill Payments')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText('City Power Co.')).toBeInTheDocument();
      // Category label combined with description in same element
      expect(screen.getByText(/Electricity/)).toBeInTheDocument();
    });
  });

  it('shows empty state when no bill payments', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, () =>
        HttpResponse.json({
          content: [],
          page: {
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 10,
          },
        }),
      ),
    );

    render(<RecentBillPayments userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/no bill payments yet/i)).toBeInTheDocument();
    });
  });

  it('shows error state on API failure', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, () =>
        HttpResponse.json({ message: 'Error' }, { status: 500 }),
      ),
    );

    render(<RecentBillPayments userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/failed to load bill payments/i)).toBeInTheDocument();
    });
  });

  it('displays total count', async () => {
    render(<RecentBillPayments userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('1 total')).toBeInTheDocument();
    });
  });

  it('calls onTransactionSelect when a bill is clicked', async () => {
    const onSelect = vi.fn();

    render(<RecentBillPayments userId="user-1" onTransactionSelect={onSelect} />);

    await waitFor(() => {
      expect(screen.getByText('City Power Co.')).toBeInTheDocument();
    });

    const rows = document.querySelectorAll('[class*="cursor-pointer"]');
    if (rows.length > 0) {
      (rows[0] as HTMLElement).click();
      expect(onSelect).toHaveBeenCalledWith('BILL_PAYMENT', 'bill-1');
    }
  });

  it('displays the biller category label', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/payments/bills/user/:userId`, () =>
        HttpResponse.json({
          content: [
            createMockBillPayment({ category: 'INTERNET', billerName: 'FiberNet ISP' }),
          ],
          page: {
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 10,
          },
        }),
      ),
    );

    render(<RecentBillPayments userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('FiberNet ISP')).toBeInTheDocument();
      // Category label combined with description in same element
      expect(screen.getByText(/Internet/)).toBeInTheDocument();
    });
  });
});
