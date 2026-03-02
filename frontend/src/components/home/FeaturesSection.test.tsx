import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';

import FeaturesSection from './FeaturesSection';
import { renderWithProviders } from '../../test/test-utils';

describe('FeaturesSection', () => {
  it('renders the section heading', () => {
    renderWithProviders(<FeaturesSection />);

    expect(screen.getByText('Features')).toBeInTheDocument();
    expect(screen.getByText(/everything you need to/i)).toBeInTheDocument();
    expect(screen.getByText('Manage Money')).toBeInTheDocument();
  });

  it('renders all 6 feature cards', () => {
    renderWithProviders(<FeaturesSection />);

    expect(screen.getByText('Virtual & Physical Cards')).toBeInTheDocument();
    expect(screen.getByText('Mobile-First Experience')).toBeInTheDocument();
    expect(screen.getByText('Global Payments')).toBeInTheDocument();
    expect(screen.getByText('Advanced Analytics')).toBeInTheDocument();
    expect(screen.getByText('Recurring Payments')).toBeInTheDocument();
    expect(screen.getByText('Fraud Protection')).toBeInTheDocument();
  });

  it('renders Learn more links for each feature', () => {
    renderWithProviders(<FeaturesSection />);

    const learnMoreLinks = screen.getAllByText('Learn more');
    expect(learnMoreLinks).toHaveLength(6);
  });

  it('renders feature descriptions', () => {
    renderWithProviders(<FeaturesSection />);

    expect(
      screen.getByText(/issue unlimited virtual cards/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/ai-powered fraud detection/i),
    ).toBeInTheDocument();
  });
});
