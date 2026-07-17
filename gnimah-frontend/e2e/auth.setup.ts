import { test as setup } from '@playwright/test';
import { DEMO_CREDENTIALS, loginAs } from './helpers';

// Se connecte UNE FOIS par rôle et sauvegarde l'état de session (localStorage) sur disque.
// Les specs le rechargent ensuite via `test.use({ storageState: ... })` au lieu de se reconnecter
// à chaque test — le backend limite volontairement les tentatives de connexion répétées depuis
// une même IP (RateLimitFilter), ce que ferait exploser un login par test.
for (const role of Object.keys(DEMO_CREDENTIALS) as (keyof typeof DEMO_CREDENTIALS)[]) {
  setup(`authenticate as ${role}`, async ({ page }) => {
    await loginAs(page, role);
    await page.context().storageState({ path: `e2e/.auth/${role}.json` });
  });
}
