import { describe, it, expect, vi, afterEach } from 'vitest';
import { screen } from '@testing-library/react';

import AccountDetailsStep from './AccountDetailsStep';
import { renderWithProviders } from '../../test/test-utils';

// Mock the SubmitButton since it uses useFormStatus which needs a parent <form action>
vi.mock('./SubmitButton', () => ({
  default: ({ disabled }: { disabled?: boolean }) => (
    <button type="submit" disabled={disabled} data-testid="submit-btn">
      Create account
    </button>
  ),
}));

describe('AccountDetailsStep', () => {
  const formAction = vi.fn();
  const onBack = vi.fn();
  const handleOAuthLogin = vi.fn();

  const defaultProps = {
    error: null,
    formAction,
    onBack,
    handleOAuthLogin,
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the form fields', () => {
    renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  it('renders OAuth buttons', () => {
    renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    expect(screen.getByText('Sign up with Google')).toBeInTheDocument();
    expect(screen.getByText('Sign up with GitHub')).toBeInTheDocument();
  });

  it('calls handleOAuthLogin when OAuth buttons are clicked', async () => {
    const { user } = renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    await user.click(screen.getByText('Sign up with Google'));
    expect(handleOAuthLogin).toHaveBeenCalledWith('google');

    await user.click(screen.getByText('Sign up with GitHub'));
    expect(handleOAuthLogin).toHaveBeenCalledWith('github');
  });

  it('displays error message when error prop is set', () => {
    renderWithProviders(
      <AccountDetailsStep {...defaultProps} error="Email already registered" />,
    );

    expect(screen.getByText('Email already registered')).toBeInTheDocument();
  });

  it('shows password requirements as user types', async () => {
    const { user } = renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    const passwordInput = screen.getByLabelText(/^password$/i);
    await user.type(passwordInput, 'abc');

    expect(screen.getByText('At least 8 characters')).toBeInTheDocument();
    expect(screen.getByText('Contains a number')).toBeInTheDocument();
    expect(screen.getByText('Contains uppercase letter')).toBeInTheDocument();
    expect(screen.getByText('Contains lowercase letter')).toBeInTheDocument();
  });

  it('shows password mismatch message', async () => {
    const { user } = renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    await user.type(screen.getByLabelText(/^password$/i), 'Password1');
    await user.type(screen.getByLabelText(/confirm password/i), 'Different1');

    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
  });

  it('submit button is disabled when passwords are invalid', () => {
    renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    expect(screen.getByTestId('submit-btn')).toBeDisabled();
  });

  it('submit button is enabled when all requirements are met', async () => {
    const { user } = renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    await user.type(screen.getByLabelText(/^password$/i), 'Password1');
    await user.type(screen.getByLabelText(/confirm password/i), 'Password1');
    await user.click(screen.getByLabelText(/i agree/i));

    expect(screen.getByTestId('submit-btn')).toBeEnabled();
  });

  it('calls onBack when Back button is clicked', async () => {
    const { user } = renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    await user.click(screen.getByRole('button', { name: /back/i }));
    expect(onBack).toHaveBeenCalledOnce();
  });

  it('renders terms and privacy links', () => {
    renderWithProviders(<AccountDetailsStep {...defaultProps} />);

    expect(screen.getByText('Terms of Service')).toBeInTheDocument();
    expect(screen.getByText('Privacy Policy')).toBeInTheDocument();
  });
});
