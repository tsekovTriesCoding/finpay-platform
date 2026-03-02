import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';

import RegisterPage from './RegisterPage';
import { renderWithProviders } from '../test/test-utils';

// Mock useAuth
const mockRegister = vi.fn();
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    register: mockRegister,
    isAuthenticated: false,
    user: null,
    isLoading: false,
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock authService
vi.mock('../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api')>();
  return {
    ...actual,
    authService: {
      getGoogleLoginUrl: () => 'https://google.com/oauth',
      getGithubLoginUrl: () => 'https://github.com/oauth',
    },
  };
});

// Mock SubmitButton
vi.mock('../components/register/SubmitButton', () => ({
  default: ({ disabled }: { disabled?: boolean }) => (
    <button type="submit" disabled={disabled} data-testid="submit-btn">
      Create account
    </button>
  ),
}));

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the create account heading', () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByText('Create your account')).toBeInTheDocument();
    expect(screen.getByText('Start managing your finances today')).toBeInTheDocument();
  });

  it('renders FinPay brand', () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByText('FinPay')).toBeInTheDocument();
  });

  it('starts at step 1 - plan selection', () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByText('Choose your plan')).toBeInTheDocument();
    expect(screen.getByText('Starter')).toBeInTheDocument();
    expect(screen.getByText('Pro')).toBeInTheDocument();
    expect(screen.getByText('Enterprise')).toBeInTheDocument();
  });

  it('navigates to step 2 after selecting a plan and clicking Continue', async () => {
    const { user } = renderWithProviders(<RegisterPage />);

    // Select Pro plan
    await user.click(screen.getByText('Pro'));
    // Click Continue
    await user.click(screen.getByRole('button', { name: /continue/i }));

    // Should now see Account Details step
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByText('Sign up with Google')).toBeInTheDocument();
  });

  it('displays selected plan info on step 2', async () => {
    const { user } = renderWithProviders(<RegisterPage />);

    await user.click(screen.getByText('Pro'));
    await user.click(screen.getByRole('button', { name: /continue/i }));

    // Plan badge should show
    expect(screen.getByText('Pro')).toBeInTheDocument();
    expect(screen.getByText('Change')).toBeInTheDocument();
  });

  it('has login link', () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByText(/already have an account/i)).toBeInTheDocument();
  });

  it('renders step indicator', () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByText('Choose Plan')).toBeInTheDocument();
    expect(screen.getByText('Create Account')).toBeInTheDocument();
  });
});
