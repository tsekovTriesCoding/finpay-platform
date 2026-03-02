import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { render, userEvent } from '../test/test-utils';
import { server } from '../test/msw-server';
import { createMockUser, createMockAuthResponse } from '../test/factories';
import { AuthProvider, useAuth } from './AuthContext';

// Test component that exposes auth context
function AuthConsumer() {
  const { user, isAuthenticated, isLoading, login, logout } = useAuth();
  return (
    <div>
      <div data-testid="loading">{String(isLoading)}</div>
      <div data-testid="authenticated">{String(isAuthenticated)}</div>
      {user && <div data-testid="user-name">{user.firstName} {user.lastName}</div>}
      {user && <div data-testid="user-email">{user.email}</div>}
      <button onClick={() => login({ email: 'john@example.com', password: 'password123' })}>
        Login
      </button>
      <button onClick={() => { logout().catch(() => {}); }}>Logout</button>
    </div>
  );
}

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
});
afterAll(() => server.close());

describe('AuthContext', () => {
  it('throws error when useAuth is used outside AuthProvider', () => {
    // Suppress React error boundary logs
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => {
      render(<AuthConsumer />, { withAuth: false });
    }).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });

  it('resolves loading state after init', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    // After init completes, loading should be false
    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
  });

  it('is not authenticated when no stored user', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
  });

  it('restores user from localStorage and validates with API', async () => {
    const mockUser = createMockUser();
    localStorage.setItem('user', JSON.stringify(mockUser));

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    expect(screen.getByTestId('user-name')).toHaveTextContent('John Doe');
  });

  it('clears auth when stored user fails validation', async () => {
    localStorage.setItem('user', JSON.stringify(createMockUser()));

    server.use(
      http.get(`${BASE_URL}/api/v1/auth/me`, () =>
        HttpResponse.json({ message: 'Unauthorized' }, { status: 401 }),
      ),
    );

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('logs in successfully via login function', async () => {
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    await user.click(screen.getByText('Login'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    expect(screen.getByTestId('user-name')).toHaveTextContent('John Doe');
    expect(localStorage.getItem('user')).not.toBeNull();
  });

  it('logs out successfully', async () => {
    const user = userEvent.setup();
    localStorage.setItem('user', JSON.stringify(createMockUser()));

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    await user.click(screen.getByText('Logout'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });

    expect(localStorage.getItem('user')).toBeNull();
  });

  it('clears auth data even if logout API fails', async () => {
    const user = userEvent.setup();
    localStorage.setItem('user', JSON.stringify(createMockUser()));

    server.use(
      http.post(`${BASE_URL}/api/v1/auth/logout`, () =>
        HttpResponse.json({ message: 'Server Error' }, { status: 500 }),
      ),
    );

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    await user.click(screen.getByText('Logout'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });
  });

  it('stores login response in localStorage', async () => {
    const user = userEvent.setup();
    const mockResponse = createMockAuthResponse();

    server.use(
      http.post(`${BASE_URL}/api/v1/auth/login`, () =>
        HttpResponse.json(mockResponse),
      ),
    );

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    await user.click(screen.getByText('Login'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    const stored = JSON.parse(localStorage.getItem('user')!);
    expect(stored.email).toBe('john@example.com');
  });
});
