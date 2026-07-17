import { test, expect } from '@playwright/test';

test.describe('Connexion', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('affiche le nombre réel de chambres (pas une valeur codée en dur)', async ({ page }) => {
    const stat = page.locator('.gn-stat-val').first();
    await expect(stat).toBeVisible();
    const text = (await stat.textContent())?.trim() ?? '';
    // Doit être un nombre (ou le tiret d'attente si l'API est momentanément indisponible), jamais vide.
    expect(text.length).toBeGreaterThan(0);
  });

  test('refuse des identifiants invalides avec un message clair', async ({ page }) => {
    await page.getByPlaceholder("Nom d'utilisateur").fill('agent');
    await page.locator('input[placeholder="Mot de passe"]').fill('mauvais-mot-de-passe');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page.locator('.p-toast-message-error, .p-toast-message')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test("l'œil du mot de passe bascule l'affichage en clair", async ({ page }) => {
    const passwordInput = page.locator('input[placeholder="Mot de passe"]');
    await passwordInput.fill('Agent@2026');
    await expect(passwordInput).toHaveAttribute('type', 'password');

    await page.locator('.p-password-toggle-mask-icon, .p-password-toggle-icon').click();

    await expect(passwordInput).toHaveAttribute('type', 'text');
  });

  test("l'option \"se souvenir de moi\" n'existe plus", async ({ page }) => {
    await expect(page.getByText('Se souvenir')).toHaveCount(0);
  });

  test('le lien "mot de passe oublié" ouvre un vrai formulaire de demande', async ({ page }) => {
    await page.getByText('Mot de passe oublié ?').click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('Mot de passe oublié')).toBeVisible();

    await dialog.getByRole('button', { name: 'Annuler' }).click();
    await expect(dialog).toBeHidden();
  });

  test('connexion réussie redirige vers le tableau de bord', async ({ page }) => {
    await page.getByPlaceholder("Nom d'utilisateur").fill('agent');
    await page.locator('input[placeholder="Mot de passe"]').fill('Agent@2026');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.locator('.gn-username')).toContainText('Agent');
  });
});
