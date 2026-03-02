import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';

import { render } from '../../test/test-utils';
import StepIndicator from './StepIndicator';

describe('StepIndicator', () => {
  it('renders two step labels', () => {
    render(<StepIndicator currentStep={1} />);

    expect(screen.getByText('Choose Plan')).toBeInTheDocument();
    expect(screen.getByText('Create Account')).toBeInTheDocument();
  });

  it('marks step 1 as active when currentStep is 1', () => {
    render(<StepIndicator currentStep={1} />);

    const step1 = screen.getByText('1');
    expect(step1.closest('div')).toHaveClass('bg-primary-500');
  });

  it('marks step 2 as active when currentStep is 2', () => {
    render(<StepIndicator currentStep={2} />);

    const step2 = screen.getByText('2');
    expect(step2.closest('div')).toHaveClass('bg-primary-500');
  });
});
