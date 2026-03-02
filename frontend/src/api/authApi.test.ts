import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { http, HttpResponse } from 'msw';

import { server } from '../test/msw-server';
import { authService } from './authApi';

const BASE_URL = 'http://localhost:8080';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('authService', () => {
  describe('login', () => {
    it('sends credentials and returns auth response', async () => {
      const response = await authService.login({
        email: 'john@example.com',
        password: 'password123',
      });

      expect(response.user.email).toBe('john@example.com');
      expect(response.tokenType).toBe('Bearer');
    });

    it('throws on invalid credentials', async () => {
      server.use(
        http.post(`${BASE_URL}/api/v1/auth/login`, () =>
          HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 }),
        ),
      );

      await expect(
        authService.login({ email: 'wrong@example.com', password: 'wrong' }),
      ).rejects.toThrow();
    });
  });

  describe('register', () => {
    it('creates a new account', async () => {
      const response = await authService.register({
        email: 'new@example.com',
        password: 'password123',
        firstName: 'New',
        lastName: 'User',
        plan: 'STARTER',
      });

      expect(response.user).toBeDefined();
      expect(response.tokenType).toBe('Bearer');
    });
  });

  describe('getCurrentUser', () => {
    it('returns the current user', async () => {
      const user = await authService.getCurrentUser();

      expect(user.email).toBe('john@example.com');
      expect(user.firstName).toBe('John');
    });

    it('throws on 401', async () => {
      server.use(
        http.get(`${BASE_URL}/api/v1/auth/me`, () =>
          HttpResponse.json({ message: 'Unauthorized' }, { status: 401 }),
        ),
      );

      await expect(authService.getCurrentUser()).rejects.toThrow();
    });
  });

  describe('logout', () => {
    it('calls the logout endpoint', async () => {
      await expect(authService.logout()).resolves.not.toThrow();
    });
  });

  describe('upgradePlan', () => {
    it('upgrades the user plan', async () => {
      const response = await authService.upgradePlan({ newPlan: 'PRO' });

      expect(response.newPlan).toBe('PRO');
      expect(response.previousPlan).toBe('STARTER');
    });
  });

  describe('OAuth URLs', () => {
    it('returns Google OAuth URL', () => {
      const url = authService.getGoogleLoginUrl();
      expect(url).toContain('/oauth2/authorization/google');
    });

    it('returns GitHub OAuth URL', () => {
      const url = authService.getGithubLoginUrl();
      expect(url).toContain('/oauth2/authorization/github');
    });
  });
});
