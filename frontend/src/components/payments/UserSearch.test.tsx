import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';

import UserSearch from './UserSearch';
import { renderWithProviders } from '../../test/test-utils';
import { createMockUserSearchResult } from '../../test/factories';

// Mock the useUserSearch hook
const mockSearch = vi.fn();
const mockClear = vi.fn();
const mockResults = [
  createMockUserSearchResult({ id: '1', firstName: 'Alice', lastName: 'Smith', email: 'alice@mail.com' }),
  createMockUserSearchResult({ id: '2', firstName: 'Bob', lastName: 'Jones', email: 'bob@mail.com' }),
];

vi.mock('../../hooks', () => ({
  useUserSearch: () => ({
    query: '',
    debouncedQuery: '',
    results: mockResults,
    isLoading: false,
    error: null,
    search: mockSearch,
    clear: mockClear,
  }),
}));

describe('UserSearch', () => {
  const onUserSelect = vi.fn();
  const onClear = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders search input when no user is selected', () => {
    renderWithProviders(
      <UserSearch
        excludeUserId="me"
        onUserSelect={onUserSelect}
        selectedUser={null}
        onClear={onClear}
      />,
    );

    expect(screen.getByPlaceholderText(/search by name or email/i)).toBeInTheDocument();
  });

  it('shows selected user card when user is provided', () => {
    const selectedUser = createMockUserSearchResult({
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@mail.com',
    });

    renderWithProviders(
      <UserSearch
        excludeUserId="me"
        onUserSelect={onUserSelect}
        selectedUser={selectedUser}
        onClear={onClear}
      />,
    );

    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByText('alice@mail.com')).toBeInTheDocument();
  });

  it('calls onClear when clear button is clicked on selected user', async () => {
    const selectedUser = createMockUserSearchResult({
      firstName: 'Alice',
      lastName: 'Smith',
    });

    const { user } = renderWithProviders(
      <UserSearch
        excludeUserId="me"
        onUserSelect={onUserSelect}
        selectedUser={selectedUser}
        onClear={onClear}
      />,
    );

    // Click the X/clear button
    const clearBtn = screen.getByRole('button');
    await user.click(clearBtn);

    expect(onClear).toHaveBeenCalled();
  });

  it('shows initials when no profile image', () => {
    const selectedUser = createMockUserSearchResult({
      firstName: 'Alice',
      lastName: 'Smith',
      profileImageUrl: undefined,
    });

    renderWithProviders(
      <UserSearch
        excludeUserId="me"
        onUserSelect={onUserSelect}
        selectedUser={selectedUser}
        onClear={onClear}
      />,
    );

    expect(screen.getByText('AS')).toBeInTheDocument();
  });
});
