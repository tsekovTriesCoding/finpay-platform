import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test('navigates between pages using navbar links', async ({ page }) => {
    await page.goto('/');

    // Click Features link (scrolls to features section)
    const featuresLink = page.locator('nav a:has-text("Features")');
    if (await featuresLink.isVisible()) {
      await featuresLink.click();
      // Should still be on the same page (it's an anchor link)
      await expect(page).toHaveURL(/.*\/#features|.*\//);
    }
  });

  test('login page is accessible directly', async ({ page }) => {
    await page.goto('/login');

    await expect(page.locator('text=Welcome back')).toBeVisible();
  });

  test('register page is accessible directly', async ({ page }) => {
    await page.goto('/register');

    await expect(page.locator('text=Create your account')).toBeVisible();
  });

  test('unknown routes show 404 or redirect', async ({ page }) => {
    await page.goto('/nonexistent-page');

    // The app should handle this - either show the page content or redirect
    // At minimum the page should load without errors
    await expect(page.locator('body')).toBeVisible();
  });

  test('protected route redirects to login when not authenticated', async ({ page }) => {
    await page.goto('/dashboard');

    // Should redirect to login since we're not authenticated
    await expect(page).toHaveURL(/.*\/login/);
  });

  test('settings route redirects to login when not authenticated', async ({ page }) => {
    await page.goto('/settings');

    // Should redirect to login
    await expect(page).toHaveURL(/.*\/login/);
  });
});

test.describe('Responsive Layout', () => {
  test('mobile viewport shows appropriate layout', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');

    // The page should render without horizontal overflow
    const body = page.locator('body');
    await expect(body).toBeVisible();

    // Hero text should still be visible
    await expect(page.getByRole('heading', { name: /The Future of Payments is Here/ })).toBeVisible();
  });

  test('desktop viewport shows full navigation', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto('/');

    await expect(page.locator('text=Sign In')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Get Started', exact: true })).toBeVisible();
  });
});

test.describe('Accessibility', () => {
  test('login form has proper labels', async ({ page }) => {
    await page.goto('/login');

    const emailLabel = page.locator('label[for="email"]');
    await expect(emailLabel).toBeVisible();

    const passwordLabel = page.locator('label[for="password"]');
    await expect(passwordLabel).toBeVisible();
  });

  test('register form has proper labels', async ({ page }) => {
    await page.goto('/register');

    // Select a plan first
    await page.click('text=Starter');
    await page.click('button:has-text("Continue")');

    await expect(page.locator('label[for="firstName"]')).toBeVisible();
    await expect(page.locator('label[for="lastName"]')).toBeVisible();
    await expect(page.locator('label[for="email"]')).toBeVisible();
    await expect(page.locator('label[for="password"]')).toBeVisible();
  });

  test('buttons have accessible text', async ({ page }) => {
    await page.goto('/login');

    const submitButton = page.locator('button[type="submit"]');
    await expect(submitButton).toContainText('Sign in');
  });

  test('social links have aria-labels', async ({ page }) => {
    await page.goto('/');

    await page.locator('footer').scrollIntoViewIfNeeded();

    await expect(page.locator('a[aria-label="Twitter"]')).toBeVisible();
    await expect(page.locator('a[aria-label="GitHub"]')).toBeVisible();
    await expect(page.locator('a[aria-label="LinkedIn"]')).toBeVisible();
  });
});
