import { test, expect } from '@playwright/test';

test.describe('Home Page', () => {
  test('displays the hero section', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('text=The Future of')).toBeVisible();
    await expect(page.locator('text=Payments')).toBeVisible();
    await expect(page.locator('text=is Here')).toBeVisible();
  });

  test('displays the navigation bar', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('text=FinPay').first()).toBeVisible();
    await expect(page.locator('text=Sign In')).toBeVisible();
    await expect(page.locator('text=Get Started')).toBeVisible();
  });

  test('navigates to login page', async ({ page }) => {
    await page.goto('/');

    await page.click('text=Sign In');
    await expect(page).toHaveURL(/.*\/login/);
    await expect(page.locator('text=Welcome back')).toBeVisible();
  });

  test('navigates to register page', async ({ page }) => {
    await page.goto('/');

    await page.click('text=Get Started');
    await expect(page).toHaveURL(/.*\/register/);
    await expect(page.locator('text=Create your account')).toBeVisible();
  });

  test('displays feature section', async ({ page }) => {
    await page.goto('/');

    // Scroll to features
    await page.locator('#features').scrollIntoViewIfNeeded();
    await expect(page.locator('text=Virtual & Physical Cards')).toBeVisible();
    await expect(page.locator('text=Global Payments')).toBeVisible();
  });

  test('displays the footer', async ({ page }) => {
    await page.goto('/');

    await page.locator('footer').scrollIntoViewIfNeeded();
    await expect(page.locator('footer')).toBeVisible();
    await expect(page.locator('footer').locator('text=Product')).toBeVisible();
    await expect(page.locator('footer').locator('text=Company')).toBeVisible();
  });

  test('hero CTA links to register for unauthenticated users', async ({ page }) => {
    await page.goto('/');

    const cta = page.locator('a:has-text("Start Free Trial")');
    await expect(cta).toBeVisible();
    await expect(cta).toHaveAttribute('href', '/register');
  });
});
