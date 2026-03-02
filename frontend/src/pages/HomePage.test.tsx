import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';

import HomePage from './HomePage';
import { renderWithProviders } from '../test/test-utils';

// Mock all home sections to isolate tests
vi.mock('../components/home/HeroSection', () => ({
  default: () => <div data-testid="hero-section">Hero</div>,
}));
vi.mock('../components/home/StatsSection', () => ({
  default: () => <div data-testid="stats-section">Stats</div>,
}));
vi.mock('../components/home/FeaturesSection', () => ({
  default: () => <div data-testid="features-section">Features</div>,
}));
vi.mock('../components/home/SecuritySection', () => ({
  default: () => <div data-testid="security-section">Security</div>,
}));
vi.mock('../components/home/CTASection', () => ({
  default: () => <div data-testid="cta-section">CTA</div>,
}));

describe('HomePage', () => {
  it('renders all sections in order', () => {
    renderWithProviders(<HomePage />);

    expect(screen.getByTestId('hero-section')).toBeInTheDocument();
    expect(screen.getByTestId('stats-section')).toBeInTheDocument();
    expect(screen.getByTestId('features-section')).toBeInTheDocument();
    expect(screen.getByTestId('security-section')).toBeInTheDocument();
    expect(screen.getByTestId('cta-section')).toBeInTheDocument();
  });

  it('renders sections in correct order', () => {
    const { container } = renderWithProviders(<HomePage />);

    const sections = container.querySelectorAll('[data-testid]');
    expect(sections[0].getAttribute('data-testid')).toBe('hero-section');
    expect(sections[1].getAttribute('data-testid')).toBe('stats-section');
    expect(sections[2].getAttribute('data-testid')).toBe('features-section');
    expect(sections[3].getAttribute('data-testid')).toBe('security-section');
    expect(sections[4].getAttribute('data-testid')).toBe('cta-section');
  });
});
