import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../test/test-utils';
import UnauthorizedPage from './UnauthorizedPage';

// Mock framer-motion to avoid animation-related test issues
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>) => (
      <div {...props}>{children}</div>
    ),
  },
}));

const mockLogout = vi.fn();
const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

describe('UnauthorizedPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('when user is authenticated', () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        isAuthenticated: true,
        logout: mockLogout,
      });
    });

    it('renders the Access Denied heading', () => {
      renderWithProviders(<UnauthorizedPage />);

      expect(screen.getByText('Access Denied')).toBeInTheDocument();
    });

    it('renders the permission denied explanation', () => {
      renderWithProviders(<UnauthorizedPage />);

      expect(
        screen.getByText(/don't have permission to view this page/i),
      ).toBeInTheDocument();
    });

    it('renders a Back to Home link pointing to /', () => {
      renderWithProviders(<UnauthorizedPage />);

      const backLink = screen.getByRole('link', { name: /back to home/i });
      expect(backLink).toBeInTheDocument();
      expect(backLink).toHaveAttribute('href', '/');
    });

    it('renders Switch Account button instead of Sign In', () => {
      renderWithProviders(<UnauthorizedPage />);

      expect(screen.getByRole('button', { name: /switch account/i })).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /sign in/i })).not.toBeInTheDocument();
    });

    it('calls logout when Switch Account is clicked', async () => {
      const { user } = renderWithProviders(<UnauthorizedPage />);

      await user.click(screen.getByRole('button', { name: /switch account/i }));

      expect(mockLogout).toHaveBeenCalledOnce();
    });
  });

  describe('when user is not authenticated', () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        isAuthenticated: false,
        logout: mockLogout,
      });
    });

    it('renders Sign In link instead of Switch Account', () => {
      renderWithProviders(<UnauthorizedPage />);

      const signInLink = screen.getByRole('link', { name: /sign in/i });
      expect(signInLink).toBeInTheDocument();
      expect(signInLink).toHaveAttribute('href', '/login');
      expect(screen.queryByRole('button', { name: /switch account/i })).not.toBeInTheDocument();
    });

    it('renders Back to Home link', () => {
      renderWithProviders(<UnauthorizedPage />);

      expect(screen.getByRole('link', { name: /back to home/i })).toBeInTheDocument();
    });
  });
});
