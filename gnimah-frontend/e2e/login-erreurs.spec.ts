import { test, expect, Page } from '@playwright/test';

/**
 * Retours d'erreur de l'écran de connexion.
 *
 * Les réponses du serveur sont simulées : le backend limite les tentatives
 * répétées (10 par tranche de 5 minutes), et une suite qui enchaîne les échecs
 * finirait par recevoir un 429 au lieu du cas testé. L'interception rend chaque
 * cas déterministe et n'entame pas ce quota.
 */
test.use({ viewport: { width: 1280, height: 900 } });

async function soumettre(page: Page, u = 'directeur', p = 'peu-importe') {
  await page.getByPlaceholder("Nom d'utilisateur").fill(u);
  await page.locator('input[placeholder="Mot de passe"]').fill(p);
  await page.getByRole('button', { name: /Se connecter|Connexion/ }).click();
}

async function simuler(page: Page, statut: number, corps: unknown = {}) {
  await page.route('**/api/auth/login', route =>
    route.fulfill({ status: statut, contentType: 'application/json', body: JSON.stringify(corps) }));
}

test('champs vides : un message sous chaque champ, sans appel au serveur', async ({ page }) => {
  let appels = 0;
  await page.route('**/api/auth/login', r => { appels++; r.continue(); });
  await page.goto('/login');
  await page.getByRole('button', { name: 'Se connecter' }).click();

  await expect(page.getByText('Saisissez votre identifiant.')).toBeVisible();
  await expect(page.getByText('Saisissez votre mot de passe.')).toBeVisible();
  expect(appels).toBe(0);
});

test('identifiants incorrects : message explicite et champs signalés', async ({ page }) => {
  await page.goto('/login');
  await simuler(page, 401, { status: 401, message: 'Identifiants invalides' });
  await soumettre(page);

  await expect(page.locator('.gn-loginerr strong'))
    .toHaveText(/Nom d’utilisateur ou mot de passe incorrect/);
  // Les deux champs sont teintés : le serveur ne révèle pas lequel est faux,
  // pour ne pas confirmer quels comptes existent.
  await expect(page.locator('.gn-field-err')).toHaveCount(2);

  // Toute frappe efface le message : il ne concerne plus la saisie en cours.
  await page.getByPlaceholder("Nom d'utilisateur").fill('autre');
  await expect(page.locator('.gn-loginerr')).toHaveCount(0);
  await expect(page.locator('.gn-field-err')).toHaveCount(0);
});

test('serveur injoignable : on parle de connexion, pas d’identifiants', async ({ page }) => {
  await page.goto('/login');
  await page.route('**/api/auth/login', route => route.abort('failed'));
  await soumettre(page);

  await expect(page.locator('.gn-loginerr strong')).toHaveText('Serveur injoignable');
  await expect(page.locator('.gn-loginerr span')).toContainText('connexion internet');
});

test('trop de tentatives : le message du limiteur est repris', async ({ page }) => {
  await page.goto('/login');
  await simuler(page, 429, { status: 429, message: 'Trop de tentatives, réessayez dans quelques minutes.' });
  await soumettre(page);

  await expect(page.locator('.gn-loginerr strong')).toHaveText('Trop de tentatives');
  await expect(page.locator('.gn-loginerr span')).toContainText('réessayez dans quelques minutes');
});

test('compte refusé : oriente vers un administrateur', async ({ page }) => {
  await page.goto('/login');
  await simuler(page, 403, { status: 403, message: 'Accès refusé' });
  await soumettre(page);

  await expect(page.locator('.gn-loginerr strong')).toHaveText('Accès refusé');
  await expect(page.locator('.gn-loginerr span')).toContainText('administrateur');
});

test('panne serveur : message dédié, pas « identifiants invalides »', async ({ page }) => {
  await page.goto('/login');
  await simuler(page, 500);
  await soumettre(page);

  await expect(page.locator('.gn-loginerr strong')).toHaveText(/serveur a rencontré un problème/);
});

test('validation du serveur : les messages se posent sur les champs', async ({ page }) => {
  await page.goto('/login');
  await simuler(page, 400, {
    status: 400, message: 'Erreur de validation',
    errors: { username: "Le nom d'utilisateur est requis" }
  });
  await soumettre(page);

  await expect(page.getByText("Le nom d'utilisateur est requis")).toBeVisible();
});

test('les icônes restent affichées si le CDN externe est inaccessible', async ({ page }) => {
  // Régression : les icônes venaient d'un CDN tiers ; son indisponibilité
  // vidait toute l'interface de ses icônes (boutons ronds vides).
  await page.route('**://cdn.jsdelivr.net/**', r => r.abort());
  await page.route('**://cdnjs.cloudflare.com/**', r => r.abort());
  await page.goto('/login');
  await page.waitForTimeout(1500);

  const icone = await page.evaluate(() => {
    const i = document.querySelector('.gn-field-icon i.ti-user') as HTMLElement | null;
    if (!i) return null;
    return { police: getComputedStyle(i).fontFamily, largeur: i.getBoundingClientRect().width };
  });

  expect(icone).not.toBeNull();
  expect(icone!.police).toContain('tabler-icons');
  // Une police absente laisserait un glyphe sans largeur.
  expect(icone!.largeur).toBeGreaterThan(6);
});

test('le logo de la résidence remplace l’initiale', async ({ page }) => {
  await page.goto('/login');
  const logo = page.locator('.gn-hero-logo img');
  await expect(logo).toBeVisible();
  // naturalWidth à 0 = image cassée.
  expect(await logo.evaluate((e: HTMLImageElement) => e.naturalWidth)).toBeGreaterThan(0);

  const favicons = await page.evaluate(() =>
    [...document.querySelectorAll('link[rel*="icon"]')].map(l => l.getAttribute('href')));
  expect(favicons.some(h => h?.includes('brand/favicon.svg'))).toBe(true);
  expect(favicons.some(h => h?.endsWith('favicon.ico'))).toBe(false);
});
