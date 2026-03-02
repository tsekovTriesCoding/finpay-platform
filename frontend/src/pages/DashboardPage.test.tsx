import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';

import DashboardPage from './DashboardPage';
import { renderWithProviders } from '../test/test-utils';
import { createMockUser } from '../test/factories';

// Mock useAuth
const mockUser = createMockUser();
const mockLogout = vi.fn();
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    logout: mockLogout,
    isAuthenticated: true,
    isLoading: false,
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock child components to isolate DashboardPage logic
vi.mock('../components/dashboard', () => ({
  DashboardHeader: ({ user, onLogout }: { user: typeof mockUser; onLogout: () => void }) => (
    <div data-testid="dashboard-header">
      <span>{user.firstName}</span>
      <button onClick={onLogout}>Logout</button>
    </div>
  ),
  WalletCard: ({ userId }: { userId: string }) => (
    <div data-testid="wallet-card">{userId}</div>
  ),
  QuickActions: ({ onSendMoney, onRequestMoney, onPayBills }: { onSendMoney: () => void; onRequestMoney: () => void; onPayBills: () => void }) => (
    <div data-testid="quick-actions">
      <button onClick={onSendMoney}>Send Money</button>
      <button onClick={onRequestMoney}>Request Money</button>
      <button onClick={onPayBills}>Pay Bills</button>
    </div>
  ),
  RecentTransfers: () => <div data-testid="recent-transfers" />,
  RecentBillPayments: () => <div data-testid="recent-bills" />,
}));

vi.mock('../components/payments', () => ({
  SendMoneyModal: ({ isOpen }: { isOpen: boolean }) => (
    isOpen ? <div data-testid="send-money-modal">Send Money Modal</div> : null
  ),
  RequestMoneyModal: ({ isOpen }: { isOpen: boolean }) => (
    isOpen ? <div data-testid="request-money-modal">Request Money Modal</div> : null
  ),
  PayBillModal: ({ isOpen }: { isOpen: boolean }) => (
    isOpen ? <div data-testid="pay-bill-modal">Pay Bill Modal</div> : null
  ),
  PendingRequestsPanel: () => <div data-testid="pending-requests" />,
  TransactionDetailSheet: () => null,
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the welcome message', () => {
    renderWithProviders(<DashboardPage />);

    expect(screen.getByText(`Welcome back, ${mockUser.firstName}!`)).toBeInTheDocument();
    expect(screen.getByText("Here's your financial overview")).toBeInTheDocument();
  });

  it('renders all dashboard sections', () => {
    renderWithProviders(<DashboardPage />);

    expect(screen.getByTestId('dashboard-header')).toBeInTheDocument();
    expect(screen.getByTestId('wallet-card')).toBeInTheDocument();
    expect(screen.getByTestId('quick-actions')).toBeInTheDocument();
    expect(screen.getByTestId('pending-requests')).toBeInTheDocument();
    expect(screen.getByTestId('recent-transfers')).toBeInTheDocument();
    expect(screen.getByTestId('recent-bills')).toBeInTheDocument();
  });

  it('opens Send Money modal when Quick Action is clicked', async () => {
    const { user } = renderWithProviders(<DashboardPage />);

    expect(screen.queryByTestId('send-money-modal')).not.toBeInTheDocument();

    await user.click(screen.getByText('Send Money'));

    expect(screen.getByTestId('send-money-modal')).toBeInTheDocument();
  });

  it('opens Request Money modal when Quick Action is clicked', async () => {
    const { user } = renderWithProviders(<DashboardPage />);

    await user.click(screen.getByText('Request Money'));

    expect(screen.getByTestId('request-money-modal')).toBeInTheDocument();
  });

  it('opens Pay Bill modal when Quick Action is clicked', async () => {
    const { user } = renderWithProviders(<DashboardPage />);

    await user.click(screen.getByText('Pay Bills'));

    expect(screen.getByTestId('pay-bill-modal')).toBeInTheDocument();
  });

  it('has Back to Home link', () => {
    renderWithProviders(<DashboardPage />);

    expect(screen.getByText(/back to home/i)).toBeInTheDocument();
  });

  it('passes logout to DashboardHeader', async () => {
    const { user } = renderWithProviders(<DashboardPage />);

    await user.click(screen.getByText('Logout'));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
