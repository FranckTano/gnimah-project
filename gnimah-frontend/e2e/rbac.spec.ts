import { test, expect } from '@playwright/test';

test.describe("Restrictions d'accès selon le rôle (RBAC, bout en bout dans l'UI réelle)", () => {
  test.describe('Agent', () => {
    test.use({ storageState: 'e2e/.auth/agent.json' });

    test('ne voit ni Statistiques & KPI, ni Utilisateurs dans le menu', async ({ page }) => {
      await page.goto('/dashboard');

      await expect(page.getByRole('link', { name: 'Statistiques & KPI' })).toHaveCount(0);
      await expect(page.getByRole('link', { name: 'Utilisateurs' })).toHaveCount(0);
      // Pas de `getByRole(..., { name: 'Chambres', exact: true })` : l'icône du lien (police d'icônes)
      // s'ajoute au nom accessible, donc une correspondance exacte échoue même quand le lien existe bien.
      await expect(page.locator('a[href="/chambres/liste"]')).toBeVisible();
    });

    test('le bouton "Nouvelle tâche" est masqué sur Housekeeping', async ({ page }) => {
      await page.goto('/entretien');

      await expect(page.getByRole('button', { name: 'Nouvelle tâche' })).toHaveCount(0);
    });
  });

  test.describe('Responsable', () => {
    test.use({ storageState: 'e2e/.auth/responsable.json' });

    test('le bouton "Nouvelle tâche" est visible et ouvre le formulaire de création', async ({ page }) => {
      await page.goto('/entretien');

      const newTaskButton = page.getByRole('button', { name: 'Nouvelle tâche' });
      await expect(newTaskButton).toBeVisible();
      await newTaskButton.click();

      const dialog = page.getByRole('dialog');
      await expect(dialog).toBeVisible();
      await expect(dialog.getByText("Nouvelle tâche d'entretien")).toBeVisible();
    });
  });

  test.describe('Directeur', () => {
    test.use({ storageState: 'e2e/.auth/directeur.json' });

    test('accède au tableau de bord Statistiques & KPI', async ({ page }) => {
      await page.goto('/dashboard');
      await page.getByRole('link', { name: 'Statistiques & KPI' }).click();

      await expect(page).toHaveURL(/\/kpi/);
      await expect(page.getByText("Chiffre d'affaires", { exact: true })).toBeVisible();
    });

    test('accéder directement à /administration/utilisateurs par URL est refusé', async ({ page }) => {
      await page.goto('/administration/utilisateurs');

      // Le guard renvoie vers /dashboard — un Directeur ne gère pas le personnel.
      await expect(page).toHaveURL(/\/dashboard/);
    });
  });

  test.describe('Admin', () => {
    test.use({ storageState: 'e2e/.auth/admin.json' });

    test('accède à la gestion des utilisateurs et voit le panneau de réinitialisation', async ({ page }) => {
      await page.goto('/administration/utilisateurs');

      await expect(page.getByText('Personnel', { exact: true })).toBeVisible();
    });
  });
});
