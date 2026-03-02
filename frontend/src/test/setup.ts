import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

// Cleanup after each test (React Testing Library best practice)
afterEach(() => {
  cleanup();
});

// Mock IntersectionObserver (used by framer-motion)
class MockIntersectionObserver {
  readonly root: Element | null = null;
  readonly rootMargin: string = '';
  readonly thresholds: ReadonlyArray<number> = [];
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
  takeRecords(): IntersectionObserverEntry[] { return []; }
}
Object.defineProperty(window, 'IntersectionObserver', {
  writable: true,
  configurable: true,
  value: MockIntersectionObserver,
});

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock scrollTo
Object.defineProperty(window, 'scrollTo', {
  writable: true,
  value: vi.fn(),
});

// Mock ResizeObserver
class MockResizeObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
}
Object.defineProperty(window, 'ResizeObserver', {
  writable: true,
  configurable: true,
  value: MockResizeObserver,
});

// Suppress console.error from act warnings during tests
const originalConsoleError = console.error;
console.error = (...args: Parameters<typeof console.error>) => {
  if (
    typeof args[0] === 'string' &&
    args[0].includes('Not implemented: navigation')
  ) {
    return;
  }
  originalConsoleError(...args);
};
