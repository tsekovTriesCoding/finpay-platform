import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';

import LoginPage from './LoginPage';
import { renderWithProviders } from '../test/test-utils';

// Mock useAuth
const mockLogin = vi.fn();
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    login: mockLogin,
    isAuthenticated: false,
    user: null,
    isLoading: false,
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock authService
vi.mock('../api', () => ({
  authService: {
    getGoogleLoginUrl: () => 'https://google.com/oauth',
    getGithubLoginUrl: () => 'https://github.com/oauth',
  },
}));

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders login form', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText('Welcome back')).toBeInTheDocument();
    expect(screen.getByText('Sign in to your account')).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('renders OAuth buttons', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText('Continue with Google')).toBeInTheDocument();
    expect(screen.getByText('Continue with GitHub')).toBeInTheDocument();
  });

  it('renders FinPay brand', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText('FinPay')).toBeInTheDocument();
  });

  it('has link to register page', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText(/don't have an account/i)).toBeInTheDocument();
  });

  it('has forgot password link', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText('Forgot password?')).toBeInTheDocument();
  });

  it('has remember me checkbox', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText('Remember me')).toBeInTheDocument();
  });

  it('has email and password inputs', () => {
    renderWithProviders(<LoginPage />);

    const emailInput = screen.getByLabelText(/email address/i);
    expect(emailInput).toHaveAttribute('type', 'email');
    expect(emailInput).toHaveAttribute('name', 'email');
    expect(emailInput).toBeRequired();

    const passwordInput = screen.getByLabelText(/password/i);
    expect(passwordInput).toHaveAttribute('type', 'password');
    expect(passwordInput).toHaveAttribute('name', 'password');
    expect(passwordInput).toBeRequired();
  });

  it('toggles password visibility', async () => {
    const { user } = renderWithProviders(<LoginPage />);

    const passwordInput = screen.getByLabelText(/password/i);
    expect(passwordInput).toHaveAttribute('type', 'password');

    // The toggle button is inside the password field's relative container
    const passwordContainer = passwordInput.closest('.relative')!;
    const toggleBtn = passwordContainer.querySelector('button[type="button"]')!;
    await user.click(toggleBtn);
    expect(passwordInput).toHaveAttribute('type', 'text');
  });

  it('renders submit button', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });
});
