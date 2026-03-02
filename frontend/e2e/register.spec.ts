import { test, expect } from '@playwright/test';

test.describe('Registration Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/register');
  });

  test('displays the registration page', async ({ page }) => {
    await expect(page.locator('text=Create your account')).toBeVisible();
    await expect(page.locator('text=Start managing your finances today')).toBeVisible();
  });

  test('shows step indicator with Plan and Account steps', async ({ page }) => {
    await expect(page.locator('text=Plan')).toBeVisible();
    await expect(page.locator('text=Account')).toBeVisible();
  });

  test('starts on plan selection step', async ({ page }) => {
    await expect(page.locator('text=Choose your plan')).toBeVisible();
    await expect(page.locator('text=Starter')).toBeVisible();
    await expect(page.locator('text=Pro')).toBeVisible();
    await expect(page.locator('text=Enterprise')).toBeVisible();
  });

  test('shows plan prices', async ({ page }) => {
    await expect(page.locator('text=$0')).toBeVisible();
    await expect(page.locator('text=$29')).toBeVisible();
    await expect(page.locator('text=Custom').first()).toBeVisible();
  });

  test('Continue button is disabled when no plan is selected', async ({ page }) => {
    const continueBtn = page.locator('button:has-text("Continue")');
    await expect(continueBtn).toBeDisabled();
  });

  test('selects a plan and navigates to account details', async ({ page }) => {
    // Click on the Pro plan
    await page.click('text=Pro');

    // Continue button should become enabled
    const continueBtn = page.locator('button:has-text("Continue")');
    await expect(continueBtn).toBeEnabled();

    // Click continue
    await continueBtn.click();

    // Should now see account details form
    await expect(page.locator('label:has-text("First name")')).toBeVisible();
    await expect(page.locator('label:has-text("Email address")')).toBeVisible();
    await expect(page.locator('label:has-text("Password")')).toBeVisible();
  });

  test('shows plan badge on step 2', async ({ page }) => {
    // Select Starter and continue
    await page.click('text=Starter');
    await page.click('button:has-text("Continue")');

    // Should show selected plan info
    await expect(page.locator('text=Starter')).toBeVisible();
    await expect(page.locator('text=Change')).toBeVisible();
  });

  test('can go back to plan selection from step 2', async ({ page }) => {
    await page.click('text=Pro');
    await page.click('button:has-text("Continue")');

    // Click back button
    await page.click('button:has-text("Back")');

    // Should be back on plan selection
    await expect(page.locator('text=Choose your plan')).toBeVisible();
  });

  test('shows OAuth buttons on step 2', async ({ page }) => {
    await page.click('text=Starter');
    await page.click('button:has-text("Continue")');

    await expect(page.locator('text=Sign up with Google')).toBeVisible();
    await expect(page.locator('text=Sign up with GitHub')).toBeVisible();
  });

  test('password requirements update in real-time', async ({ page }) => {
    await page.click('text=Starter');
    await page.click('button:has-text("Continue")');

    // Type a weak password
    await page.fill('input[name="password"]', 'abc');

    // Requirements should show
    await expect(page.locator('text=At least 8 characters')).toBeVisible();
    await expect(page.locator('text=Contains a number')).toBeVisible();
    await expect(page.locator('text=Contains uppercase letter')).toBeVisible();
    await expect(page.locator('text=Contains lowercase letter')).toBeVisible();
  });

  test('shows password mismatch warning', async ({ page }) => {
    await page.click('text=Starter');
    await page.click('button:has-text("Continue")');

    await page.fill('input[name="password"]', 'Password1');
    await page.fill('input[name="confirmPassword"]', 'Different1');

    await expect(page.locator('text=Passwords do not match')).toBeVisible();
  });

  test('has login link', async ({ page }) => {
    const loginLink = page.locator('a:has-text("Sign in")');
    await expect(loginLink).toBeVisible();
    await loginLink.click();
    await expect(page).toHaveURL(/.*\/login/);
  });

  test('pre-selects plan from URL parameter', async ({ page }) => {
    await page.goto('/register?plan=pro');

    // Should skip to step 2 since plan is pre-selected
    await expect(page.locator('label:has-text("First name")')).toBeVisible();
    await expect(page.locator('text=Pro')).toBeVisible();
  });
});
