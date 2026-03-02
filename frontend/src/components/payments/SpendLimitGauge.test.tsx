import { describe, it, expect } from 'vitest';

import SpendLimitGauge from './SpendLimitGauge';
import { renderWithProviders } from '../../test/test-utils';
import { screen } from '@testing-library/react';

const fmt = (v: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(v);

describe('SpendLimitGauge', () => {
  it('renders label, remaining, and limit text', () => {
    renderWithProviders(
      <SpendLimitGauge label="Daily Left" remaining={400} limit={500} suffix="day" formatCurrency={fmt} />,
    );

    expect(screen.getByText('Daily Left')).toBeInTheDocument();
    expect(screen.getByText('$400.00')).toBeInTheDocument();
    expect(screen.getByText(/of \$500\.00\/day/)).toBeInTheDocument();
  });

  it('uses green color when remaining > 25%', () => {
    const { container } = renderWithProviders(
      <SpendLimitGauge label="Left" remaining={300} limit={500} suffix="mo" formatCurrency={fmt} />,
    );

    // The progress bar should have secondary (green) color
    const bar = container.querySelector('.bg-secondary-500');
    expect(bar).toBeInTheDocument();
  });

  it('uses amber color when remaining <= 25%', () => {
    const { container } = renderWithProviders(
      <SpendLimitGauge label="Left" remaining={100} limit={500} suffix="mo" formatCurrency={fmt} />,
    );

    const bar = container.querySelector('.bg-amber-500');
    expect(bar).toBeInTheDocument();
  });

  it('uses red color when remaining <= 10%', () => {
    const { container } = renderWithProviders(
      <SpendLimitGauge label="Left" remaining={40} limit={500} suffix="mo" formatCurrency={fmt} />,
    );

    const bar = container.querySelector('.bg-red-500');
    expect(bar).toBeInTheDocument();
  });

  it('handles zero limit gracefully', () => {
    renderWithProviders(
      <SpendLimitGauge label="Left" remaining={0} limit={0} suffix="day" formatCurrency={fmt} />,
    );

    expect(screen.getByText('$0.00')).toBeInTheDocument();
  });
});
