import { test, expect } from '@playwright/test';

test.describe('Gestion des chambres (module CRUD, atteignable depuis le menu Opérations)', () => {
  test.describe('Directeur', () => {
    test.use({ storageState: 'e2e/.auth/directeur.json' });

    test('crée une chambre avec un nom personnalisé et la retrouve dans la liste', async ({ page }) => {
      await page.goto('/chambres/liste');

      // Numéro ET nom uniques par exécution : le test tourne contre une base persistante (pas réinitialisée
      // entre les runs), un nom fixe finirait par correspondre à plusieurs chambres créées par des runs précédents.
      const suffix = Date.now();
      const numero = `E2E-${suffix}`;
      const nom = `Suite Playwright ${suffix}`;
      await page.getByRole('button', { name: 'Nouvelle chambre' }).click();

      const dialog = page.getByRole('dialog');
      await dialog.locator('[formcontrolname="numero"]').fill(numero);
      await dialog.locator('[formcontrolname="nom"]').fill(nom);
      await dialog.locator('[formcontrolname="tarifPassage"]').fill('5000');
      await dialog.locator('[formcontrolname="tarifNuitee"]').fill('15000');
      await dialog.getByRole('button', { name: 'Créer' }).click();

      await expect(page.getByText(nom)).toBeVisible();
      await expect(page.getByText(numero)).toBeVisible();
    });
  });

  test.describe('Agent', () => {
    test.use({ storageState: 'e2e/.auth/agent.json' });

    test('peut consulter le plan des chambres', async ({ page }) => {
      await page.goto('/chambres');

      await expect(page.getByText('Libre', { exact: true }).first()).toBeVisible();
    });
  });
});
