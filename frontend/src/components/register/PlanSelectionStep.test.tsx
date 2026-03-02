import { describe, it, expect, vi, afterEach } from 'vitest';
import { screen } from '@testing-library/react';

import PlanSelectionStep from './PlanSelectionStep';
import { renderWithProviders } from '../../test/test-utils';

describe('PlanSelectionStep', () => {
  const onSelect = vi.fn();
  const onContinue = vi.fn();

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders all three plans', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByText('Starter')).toBeInTheDocument();
    expect(screen.getByText('Pro')).toBeInTheDocument();
    expect(screen.getByText('Enterprise')).toBeInTheDocument();
  });

  it('shows Popular badge only on Pro plan', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    const badges = screen.getAllByText('Popular');
    expect(badges).toHaveLength(1);
  });

  it('displays prices correctly', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByText('$0')).toBeInTheDocument();
    expect(screen.getByText('$29')).toBeInTheDocument();
    expect(screen.getByText('Custom')).toBeInTheDocument();
  });

  it('calls onSelect when a plan is clicked', async () => {
    const { user } = renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    await user.click(screen.getByText('Starter'));
    expect(onSelect).toHaveBeenCalledWith('STARTER');
  });

  it('disables Continue button when no plan is selected', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByRole('button', { name: /continue/i })).toBeDisabled();
  });

  it('enables Continue button when a plan is selected', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan="PRO" onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByRole('button', { name: /continue/i })).toBeEnabled();
  });

  it('calls onContinue when Continue is clicked', async () => {
    const { user } = renderWithProviders(
      <PlanSelectionStep selectedPlan="PRO" onSelect={onSelect} onContinue={onContinue} />,
    );

    await user.click(screen.getByRole('button', { name: /continue/i }));
    expect(onContinue).toHaveBeenCalledOnce();
  });

  it('shows features when a plan is selected', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan="STARTER" onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByText('Up to 10 transactions/month')).toBeInTheDocument();
    expect(screen.getByText('Email support')).toBeInTheDocument();
  });

  it('displays plan limits', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByText('$500')).toBeInTheDocument();
    expect(screen.getByText('$5,000')).toBeInTheDocument();
  });

  it('renders compare plans link', () => {
    renderWithProviders(
      <PlanSelectionStep selectedPlan={null} onSelect={onSelect} onContinue={onContinue} />,
    );

    expect(screen.getByText('Compare plans in detail')).toBeInTheDocument();
  });
});
