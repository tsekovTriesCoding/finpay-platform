import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';

import HeroSection from './HeroSection';
import { renderWithProviders } from '../../test/test-utils';

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: false,
    user: null,
    isLoading: false,
  }),
}));

describe('HeroSection', () => {
  it('renders the main headline', () => {
    renderWithProviders(<HeroSection />);

    expect(screen.getByText(/the future of/i)).toBeInTheDocument();
    expect(screen.getByText('Payments')).toBeInTheDocument();
    expect(screen.getByText(/is here/i)).toBeInTheDocument();
  });

  it('renders the description text', () => {
    renderWithProviders(<HeroSection />);

    expect(
      screen.getByText(/experience lightning-fast transactions/i),
    ).toBeInTheDocument();
  });

  it('shows Start Free Trial when unauthenticated', () => {
    renderWithProviders(<HeroSection />);

    expect(screen.getByText('Start Free Trial')).toBeInTheDocument();
  });

  it('shows feature badges', () => {
    renderWithProviders(<HeroSection />);

    expect(screen.getByText('Bank-grade Security')).toBeInTheDocument();
    expect(screen.getByText('Instant Transfers')).toBeInTheDocument();
    expect(screen.getByText('150+ Countries')).toBeInTheDocument();
  });

  it('shows Watch Demo button', () => {
    renderWithProviders(<HeroSection />);

    expect(screen.getByText('Watch Demo')).toBeInTheDocument();
  });

  it('renders dashboard preview stats', () => {
    renderWithProviders(<HeroSection />);

    expect(screen.getByText('Total Balance')).toBeInTheDocument();
    expect(screen.getByText('Monthly Revenue')).toBeInTheDocument();
  });
});

describe('HeroSection (authenticated)', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it('shows Go to Dashboard when authenticated', async () => {
    vi.doMock('../../contexts/AuthContext', () => ({
      useAuth: () => ({
        isAuthenticated: true,
        user: { id: '1', firstName: 'Test' },
        isLoading: false,
      }),
    }));

    const { default: HeroSectionAuth } = await import('./HeroSection');
    renderWithProviders(<HeroSectionAuth />);

    expect(screen.getByText('Go to Dashboard')).toBeInTheDocument();
  });
});
