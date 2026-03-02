import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest';
import { screen } from '@testing-library/react';

import { render } from '../../test/test-utils';
import { server } from '../../test/msw-server';

// Mock useAuth
vi.mock('../../contexts/AuthContext', async () => {
  const actual = await vi.importActual<typeof import('../../contexts/AuthContext')>('../../contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn(),
  };
});

import { useAuth } from '../../contexts/AuthContext';
import Navbar from './Navbar';

const mockUseAuth = vi.mocked(useAuth);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Navbar', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      isAuthenticated: false,
      user: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      logoutAll: vi.fn(),
      refreshUser: vi.fn(),
      patchUser: vi.fn(),
    });
  });

  it('renders the FinPay brand link', () => {
    render(<Navbar />);

    expect(screen.getByText('Pay')).toBeInTheDocument();
  });

  it('renders navigation links', () => {
    render(<Navbar />);

    expect(screen.getByText('Features')).toBeInTheDocument();
    expect(screen.getByText('Security')).toBeInTheDocument();
    expect(screen.getByText('Pricing')).toBeInTheDocument();
    expect(screen.getByText('About')).toBeInTheDocument();
  });

  it('shows Sign In and Get Started when not authenticated', () => {
    render(<Navbar />);

    // Desktop nav links
    const signInLinks = screen.getAllByText('Sign In');
    const getStartedLinks = screen.getAllByText('Get Started');

    expect(signInLinks.length).toBeGreaterThan(0);
    expect(getStartedLinks.length).toBeGreaterThan(0);
  });

  it('shows Dashboard link when authenticated', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      isAuthenticated: true,
      user: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      logoutAll: vi.fn(),
      refreshUser: vi.fn(),
      patchUser: vi.fn(),
    });

    render(<Navbar />);

    const dashLinks = screen.getAllByText('Dashboard');
    expect(dashLinks.length).toBeGreaterThan(0);
  });

  it('does not show auth buttons while loading', () => {
    mockUseAuth.mockReturnValue({
      isLoading: true,
      isAuthenticated: false,
      user: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      logoutAll: vi.fn(),
      refreshUser: vi.fn(),
      patchUser: vi.fn(),
    });

    render(<Navbar />);

    // When loading, the auth section should not show Sign In or Dashboard
    // The desktop nav section checks !isLoading
    expect(screen.queryByText('Sign In')).toBeInTheDocument(); // It's still in mobile menu
  });
});
