import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('displays the login form', async ({ page }) => {
    await expect(page.locator('text=Welcome back')).toBeVisible();
    await expect(page.locator('text=Sign in to your account')).toBeVisible();
    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
  });

  test('displays OAuth buttons', async ({ page }) => {
    await expect(page.locator('text=Continue with Google')).toBeVisible();
    await expect(page.locator('text=Continue with GitHub')).toBeVisible();
  });

  test('displays FinPay branding', async ({ page }) => {
    await expect(page.locator('text=FinPay').first()).toBeVisible();
  });

  test('has forgot password link', async ({ page }) => {
    await expect(page.locator('text=Forgot password?')).toBeVisible();
  });

  test('has create account link', async ({ page }) => {
    const registerLink = page.locator('a:has-text("Sign up")');
    await expect(registerLink).toBeVisible();
    await registerLink.click();
    await expect(page).toHaveURL(/.*\/register/);
  });

  test('email input requires valid email', async ({ page }) => {
    const emailInput = page.locator('input[name="email"]');
    await expect(emailInput).toHaveAttribute('type', 'email');
    await expect(emailInput).toHaveAttribute('required', '');
  });

  test('password input is masked by default', async ({ page }) => {
    const passwordInput = page.locator('input[name="password"]');
    await expect(passwordInput).toHaveAttribute('type', 'password');
  });

  test('toggle password visibility works', async ({ page }) => {
    const passwordInput = page.locator('input[name="password"]');
    await expect(passwordInput).toHaveAttribute('type', 'password');

    // Click the eye icon button (the button inside the password field container)
    const toggleBtn = page.locator('input[name="password"]').locator('..').locator('button');
    await toggleBtn.click();

    await expect(passwordInput).toHaveAttribute('type', 'text');
  });

  test('submit button is present', async ({ page }) => {
    await expect(page.locator('button[type="submit"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toContainText('Sign in');
  });
});
