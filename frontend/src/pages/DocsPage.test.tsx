import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import DocsPage from './DocsPage';
import { renderWithProviders } from '../test/test-utils';

// Mock framer-motion to avoid animation-related test issues
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>) => (
      <div {...props}>{children}</div>
    ),
  },
}));

describe('DocsPage', () => {
  it('renders the page heading', () => {
    renderWithProviders(<DocsPage />);

    expect(screen.getByText('API Documentation')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
  });

  it('renders the description mentioning SpringDoc OpenAPI', () => {
    renderWithProviders(<DocsPage />);

    expect(
      screen.getByText(/live API documentation powered by SpringDoc OpenAPI/i),
    ).toBeInTheDocument();
  });

  it('renders service selector buttons for all services', () => {
    renderWithProviders(<DocsPage />);

    expect(screen.getByRole('button', { name: 'Auth Service' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'User Service' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Payment Service' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Wallet Service' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Notification Service' })).toBeInTheDocument();
  });

  it('renders a Swagger UI iframe', () => {
    renderWithProviders(<DocsPage />);

    const iframe = screen.getByTitle('auth-service API Documentation');
    expect(iframe).toBeInTheDocument();
    expect(iframe.tagName).toBe('IFRAME');
  });

  it('defaults to Auth Service as active', () => {
    renderWithProviders(<DocsPage />);

    const iframe = screen.getByTitle('auth-service API Documentation');
    expect(iframe).toHaveAttribute('src', expect.stringContaining('Auth%20Service'));
  });

  it('switches iframe when clicking a different service', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DocsPage />);

    await user.click(screen.getByRole('button', { name: 'Payment Service' }));

    const iframe = screen.getByTitle('payment-service API Documentation');
    expect(iframe).toBeInTheDocument();
    expect(iframe).toHaveAttribute('src', expect.stringContaining('Payment%20Service'));
  });

  it('renders the "Open in new tab" link', () => {
    renderWithProviders(<DocsPage />);

    const link = screen.getByText('Open in new tab');
    expect(link).toBeInTheDocument();
    expect(link.closest('a')).toHaveAttribute('target', '_blank');
    expect(link.closest('a')).toHaveAttribute('rel', 'noopener noreferrer');
  });
});
