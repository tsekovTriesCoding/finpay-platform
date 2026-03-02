import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';

import { render, userEvent } from '../../test/test-utils';
import QuickActions from './QuickActions';

describe('QuickActions', () => {
  it('renders all four quick action buttons', () => {
    render(
      <QuickActions
        onSendMoney={vi.fn()}
        onRequestMoney={vi.fn()}
        onPayBills={vi.fn()}
      />,
    );

    expect(screen.getByText('Send Money')).toBeInTheDocument();
    expect(screen.getByText('Request Money')).toBeInTheDocument();
    expect(screen.getByText('Pay Bills')).toBeInTheDocument();
    expect(screen.getByText('Investments')).toBeInTheDocument();
  });

  it('renders the section heading', () => {
    render(
      <QuickActions
        onSendMoney={vi.fn()}
        onRequestMoney={vi.fn()}
        onPayBills={vi.fn()}
      />,
    );

    expect(screen.getByText('Quick Actions')).toBeInTheDocument();
  });

  it('calls onSendMoney when Send Money is clicked', async () => {
    const user = userEvent.setup();
    const onSendMoney = vi.fn();

    render(
      <QuickActions
        onSendMoney={onSendMoney}
        onRequestMoney={vi.fn()}
        onPayBills={vi.fn()}
      />,
    );

    await user.click(screen.getByText('Send Money'));
    expect(onSendMoney).toHaveBeenCalledTimes(1);
  });

  it('calls onRequestMoney when Request Money is clicked', async () => {
    const user = userEvent.setup();
    const onRequestMoney = vi.fn();

    render(
      <QuickActions
        onSendMoney={vi.fn()}
        onRequestMoney={onRequestMoney}
        onPayBills={vi.fn()}
      />,
    );

    await user.click(screen.getByText('Request Money'));
    expect(onRequestMoney).toHaveBeenCalledTimes(1);
  });

  it('calls onPayBills when Pay Bills is clicked', async () => {
    const user = userEvent.setup();
    const onPayBills = vi.fn();

    render(
      <QuickActions
        onSendMoney={vi.fn()}
        onRequestMoney={vi.fn()}
        onPayBills={onPayBills}
      />,
    );

    await user.click(screen.getByText('Pay Bills'));
    expect(onPayBills).toHaveBeenCalledTimes(1);
  });
});
