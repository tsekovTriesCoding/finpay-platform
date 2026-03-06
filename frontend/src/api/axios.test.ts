import { describe, it, expect, vi, beforeEach } from 'vitest';

import api, { getApiErrorMessage, API_GATEWAY_URL } from './axios';

describe('getApiErrorMessage', () => {
  it('extracts message from Axios error response', () => {
    const error = {
      response: { data: { message: 'Email already exists' } },
    };

    expect(getApiErrorMessage(error)).toBe('Email already exists');
  });

  it('falls back to Error.message', () => {
    const error = new Error('Network error');
    expect(getApiErrorMessage(error)).toBe('Network error');
  });

  it('returns custom fallback message', () => {
    expect(getApiErrorMessage({}, 'Custom fallback')).toBe('Custom fallback');
  });

  it('returns default fallback for unknown errors', () => {
    expect(getApiErrorMessage(null)).toBe('Something went wrong. Please try again.');
  });

  it('returns default fallback for undefined', () => {
    expect(getApiErrorMessage(undefined)).toBe('Something went wrong. Please try again.');
  });

  it('extracts message from deeply nested error', () => {
    const error = {
      response: { data: { message: 'Validation failed' } },
    };

    expect(getApiErrorMessage(error)).toBe('Validation failed');
  });

  it('handles error object without response.data.message', () => {
    const error = {
      response: { data: {} },
    };

    expect(getApiErrorMessage(error)).toBe('Something went wrong. Please try again.');
  });

  it('prefers response.data.message over Error.message', () => {
    const error = Object.assign(new Error('generic'), {
      response: { data: { message: 'Account is suspended' } },
    });

    expect(getApiErrorMessage(error)).toBe('Account is suspended');
  });
});

describe('axios interceptor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should have withCredentials enabled', () => {
    expect(api.defaults.withCredentials).toBe(true);
  });

  it('should set Content-Type to application/json', () => {
    expect(api.defaults.headers['Content-Type']).toBe('application/json');
  });

  it('should use API_GATEWAY_URL as baseURL', () => {
    expect(api.defaults.baseURL).toBe(API_GATEWAY_URL);
  });
});
