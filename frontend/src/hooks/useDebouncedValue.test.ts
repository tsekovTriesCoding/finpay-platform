import { describe, it, expect, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';

import { useDebouncedValue } from './useDebouncedValue';

describe('useDebouncedValue', () => {
  it('returns the initial value immediately', () => {
    const { result } = renderHook(() => useDebouncedValue('hello', 300));
    expect(result.current).toBe('hello');
  });

  it('does not update immediately when value changes', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 300),
      { initialProps: { value: 'hello' } },
    );

    rerender({ value: 'world' });
    // Should still be the old value
    expect(result.current).toBe('hello');
  });

  it('updates after the delay', async () => {
    vi.useFakeTimers();

    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 300),
      { initialProps: { value: 'hello' } },
    );

    rerender({ value: 'world' });

    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current).toBe('world');

    vi.useRealTimers();
  });

  it('resets the timer on rapid changes', async () => {
    vi.useFakeTimers();

    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 300),
      { initialProps: { value: 'a' } },
    );

    // Rapid changes
    rerender({ value: 'ab' });
    act(() => { vi.advanceTimersByTime(100); });

    rerender({ value: 'abc' });
    act(() => { vi.advanceTimersByTime(100); });

    rerender({ value: 'abcd' });

    // Not enough time for any to resolve
    expect(result.current).toBe('a');

    // Wait for the full delay
    act(() => { vi.advanceTimersByTime(300); });

    // Should be the final value
    expect(result.current).toBe('abcd');

    vi.useRealTimers();
  });

  it('works with numbers', () => {
    vi.useFakeTimers();

    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 200),
      { initialProps: { value: 0 } },
    );

    rerender({ value: 42 });
    act(() => { vi.advanceTimersByTime(200); });

    expect(result.current).toBe(42);

    vi.useRealTimers();
  });

  it('works with objects', () => {
    vi.useFakeTimers();

    const obj1 = { key: 'value1' };
    const obj2 = { key: 'value2' };

    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 200),
      { initialProps: { value: obj1 } },
    );

    rerender({ value: obj2 });
    act(() => { vi.advanceTimersByTime(200); });

    expect(result.current).toEqual(obj2);

    vi.useRealTimers();
  });
});
