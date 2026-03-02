import { render, type RenderOptions } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, type MemoryRouterProps } from 'react-router-dom';
import { type ReactElement, type ReactNode } from 'react';

import { AuthProvider } from '../contexts/AuthContext';

// Create a fresh QueryClient for each test to avoid cache leaking
function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

interface WrapperOptions {
  /** Initial route entries for MemoryRouter */
  routerEntries?: MemoryRouterProps['initialEntries'];
  /** Whether to include AuthProvider */
  withAuth?: boolean;
}

/**
 * Custom render that wraps components in the same providers
 * the real app uses (QueryClient, Router, Auth).
 *
 * Follows React Testing Library best practices:
 * - Fresh QueryClient per test (no state leaking)
 * - MemoryRouter for route-aware components
 * - Optional AuthProvider for authenticated views
 */
export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'> & WrapperOptions,
) {
  const {
    routerEntries = ['/'],
    withAuth = false,
    ...renderOptions
  } = options ?? {};

  const queryClient = createTestQueryClient();

  function Wrapper({ children }: { children: ReactNode }) {
    const content = (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={routerEntries}>
          {children}
        </MemoryRouter>
      </QueryClientProvider>
    );

    if (withAuth) {
      return (
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={routerEntries}>
            <AuthProvider>{children}</AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      );
    }

    return content;
  }

  const user = userEvent.setup();

  return {
    user,
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
    queryClient,
  };
}

// Re-export everything from testing-library
// eslint-disable-next-line react-refresh/only-export-components
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';
export { renderWithProviders as render };
