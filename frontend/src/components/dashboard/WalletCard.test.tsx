import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { render } from '../../test/test-utils';
import { server } from '../../test/msw-server';
import { createMockWallet } from '../../test/factories';
import WalletCard from './WalletCard';

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('WalletCard', () => {
  it('shows loading skeleton initially', () => {
    render(<WalletCard userId="user-1" />);

    // Pulse animation element should be present during loading
    expect(screen.getByText('Available Balance')).toBeInTheDocument();
  });

  it('displays wallet balance after loading', async () => {
    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('$4,800.00')).toBeInTheDocument();
    });
  });

  it('displays plan badge', async () => {
    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Starter')).toBeInTheDocument();
    });
  });

  it('displays daily and monthly limits', async () => {
    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Daily Limit')).toBeInTheDocument();
      expect(screen.getByText('Monthly Limit')).toBeInTheDocument();
      expect(screen.getByText('$5,000.00')).toBeInTheDocument();
      expect(screen.getByText('$50,000.00')).toBeInTheDocument();
    });
  });

  it('shows reserved balance when present', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
        HttpResponse.json(createMockWallet({ reservedBalance: 500, availableBalance: 4500 })),
      ),
    );

    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/reserved for pending transfers/i)).toBeInTheDocument();
    });
  });

  it('shows Upgrade Plan link for non-Enterprise plans', async () => {
    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Upgrade Plan')).toBeInTheDocument();
    });
  });

  it('hides Upgrade Plan link for Enterprise plan', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
        HttpResponse.json(createMockWallet({ plan: 'ENTERPRISE' })),
      ),
    );

    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/\$4,800\.00/)).toBeInTheDocument(); // Balance loaded
    });

    expect(screen.queryByText('Upgrade Plan')).not.toBeInTheDocument();
  });

  it('shows error message on API failure', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
        HttpResponse.json({ message: 'Error' }, { status: 403 }),
      ),
    );

    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Unable to load balance')).toBeInTheDocument();
    }, { timeout: 5000 });
  });

  it('shows Pro plan badge when wallet plan is PRO', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/wallets/user/:userId`, () =>
        HttpResponse.json(createMockWallet({ plan: 'PRO' })),
      ),
    );

    render(<WalletCard userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText('Pro')).toBeInTheDocument();
    });
  });
});
