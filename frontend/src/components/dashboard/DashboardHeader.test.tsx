import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';

import { render, userEvent } from '../../test/test-utils';
import { server } from '../../test/msw-server';
import { createMockUser } from '../../test/factories';
import DashboardHeader from './DashboardHeader';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('DashboardHeader', () => {
  const mockUser = createMockUser();
  const mockLogout = vi.fn().mockResolvedValue(undefined);

  it('renders user name and email', () => {
    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john@example.com')).toBeInTheDocument();
  });

  it('renders the FinPay brand', () => {
    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    expect(screen.getByText('FinPay')).toBeInTheDocument();
  });

  it('displays user initials when no profile image', () => {
    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    expect(screen.getByText('JD')).toBeInTheDocument();
  });

  it('displays profile image when available', () => {
    const userWithImage = createMockUser({ profileImageUrl: 'https://example.com/photo.jpg' });
    render(<DashboardHeader user={userWithImage} onLogout={mockLogout} />);

    const img = screen.getByAltText('John Doe');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'https://example.com/photo.jpg');
  });

  it('shows plan badge', () => {
    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    expect(screen.getByText('Starter')).toBeInTheDocument();
  });

  it('shows PRO badge for PRO plan user', () => {
    const proUser = createMockUser({ plan: 'PRO' });
    render(<DashboardHeader user={proUser} onLogout={mockLogout} />);

    expect(screen.getByText('Pro')).toBeInTheDocument();
  });

  it('has settings button', () => {
    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    expect(screen.getByTitle('Settings')).toBeInTheDocument();
  });

  it('has logout button', () => {
    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    expect(screen.getByTitle('Logout')).toBeInTheDocument();
  });

  it('calls onLogout when logout is clicked', async () => {
    const user = userEvent.setup();

    render(<DashboardHeader user={mockUser} onLogout={mockLogout} />);

    await user.click(screen.getByTitle('Logout'));

    await waitFor(() => {
      expect(mockLogout).toHaveBeenCalledTimes(1);
    });
  });
});
