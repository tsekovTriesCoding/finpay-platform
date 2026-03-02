import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';

import ProfileTab from './ProfileTab';
import { renderWithProviders } from '../../test/test-utils';
import { createMockUser } from '../../test/factories';

// Mock the userService
vi.mock('../../api/userApi', () => ({
  userService: {
    updateProfile: vi.fn().mockResolvedValue(undefined),
  },
}));

// Mock ProfileImageUpload
vi.mock('./ProfileImageUpload', () => ({
  default: () => <div data-testid="profile-image-upload">Photo Upload</div>,
}));

describe('ProfileTab', () => {
  const mockUser = createMockUser({
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    phoneNumber: '+1 555 1234',
    address: '123 Main St',
    city: 'Springfield',
    country: 'US',
    postalCode: '62701',
    status: 'ACTIVE',
    role: 'USER',
  });

  const onProfileUpdated = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders personal information fields with user data', () => {
    renderWithProviders(<ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />);

    expect(screen.getByLabelText(/first name/i)).toHaveValue('John');
    expect(screen.getByLabelText(/last name/i)).toHaveValue('Doe');
    expect(screen.getByLabelText(/email address/i)).toHaveValue('john@example.com');
    expect(screen.getByLabelText(/phone number/i)).toHaveValue('+1 555 1234');
  });

  it('renders address fields', () => {
    renderWithProviders(<ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />);

    expect(screen.getByLabelText(/street address/i)).toHaveValue('123 Main St');
    expect(screen.getByLabelText(/city/i)).toHaveValue('Springfield');
    expect(screen.getByLabelText(/country/i)).toHaveValue('US');
    expect(screen.getByLabelText(/postal code/i)).toHaveValue('62701');
  });

  it('renders profile photo upload', () => {
    renderWithProviders(<ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />);

    expect(screen.getByTestId('profile-image-upload')).toBeInTheDocument();
    expect(screen.getByText('Profile Photo')).toBeInTheDocument();
  });

  it('renders save button', () => {
    renderWithProviders(<ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />);

    expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
  });

  it('renders account details section', () => {
    renderWithProviders(<ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />);

    expect(screen.getByText('Account Details')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('updates form fields on change', async () => {
    const { user } = renderWithProviders(
      <ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />,
    );

    const firstNameInput = screen.getByLabelText(/first name/i);
    await user.clear(firstNameInput);
    await user.type(firstNameInput, 'Jane');

    expect(firstNameInput).toHaveValue('Jane');
  });

  it('shows success message after saving', async () => {
    const { userService } = await import('../../api/userApi');
    (userService.updateProfile as ReturnType<typeof vi.fn>).mockResolvedValueOnce(undefined);

    const { user } = renderWithProviders(
      <ProfileTab user={mockUser} onProfileUpdated={onProfileUpdated} />,
    );

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(screen.getByText('Profile updated successfully!')).toBeInTheDocument();
    });
  });
});
