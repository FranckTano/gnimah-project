import { Page, expect } from '@playwright/test';

export const DEMO_CREDENTIALS = {
  admin: { username: 'admin', password: 'Admin@2026' },
  directeur: { username: 'directeur', password: 'Directeur@2026' },
  responsable: { username: 'responsable', password: 'Responsable@2026' },
  agent: { username: 'agent', password: 'Agent@2026' }
} as const;

export async function loginAs(page: Page, role: keyof typeof DEMO_CREDENTIALS): Promise<void> {
  const { username, password } = DEMO_CREDENTIALS[role];
  await page.goto('/login');
  await page.getByPlaceholder("Nom d'utilisateur").fill(username);
  await page.locator('input[placeholder="Mot de passe"]').fill(password);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
}
