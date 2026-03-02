import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';

import Footer from './Footer';
import { renderWithProviders } from '../../test/test-utils';

describe('Footer', () => {
  it('renders the FinPay brand', () => {
    renderWithProviders(<Footer />);

    expect(screen.getByText('Fin')).toBeInTheDocument();
    expect(screen.getByText('Pay')).toBeInTheDocument();
  });

  it('renders product links', () => {
    renderWithProviders(<Footer />);

    expect(screen.getByText('Product')).toBeInTheDocument();
    expect(screen.getByText('Features')).toBeInTheDocument();
    expect(screen.getByText('Security')).toBeInTheDocument();
    expect(screen.getByText('Pricing')).toBeInTheDocument();
    expect(screen.getByText('API Docs')).toBeInTheDocument();
  });

  it('renders company links', () => {
    renderWithProviders(<Footer />);

    expect(screen.getByText('Company')).toBeInTheDocument();
    expect(screen.getByText('About')).toBeInTheDocument();
    expect(screen.getByText('Blog')).toBeInTheDocument();
    expect(screen.getByText('Careers')).toBeInTheDocument();
    expect(screen.getByText('Contact')).toBeInTheDocument();
  });

  it('renders legal links', () => {
    renderWithProviders(<Footer />);

    expect(screen.getByText('Legal')).toBeInTheDocument();
    expect(screen.getByText('Privacy Policy')).toBeInTheDocument();
    expect(screen.getByText('Terms of Service')).toBeInTheDocument();
  });

  it('renders social media links', () => {
    renderWithProviders(<Footer />);

    expect(screen.getByLabelText('Twitter')).toBeInTheDocument();
    expect(screen.getByLabelText('GitHub')).toBeInTheDocument();
    expect(screen.getByLabelText('LinkedIn')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
  });

  it('renders tagline', () => {
    renderWithProviders(<Footer />);

    expect(
      screen.getByText(/the future of payments/i),
    ).toBeInTheDocument();
  });
});
