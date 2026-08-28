import { test, expect } from '@playwright/test';
import { readFileSync, existsSync } from 'node:fs';

/**
 * Envoi d'une photo depuis l'interface, de bout en bout.
 *
 * L'administrateur choisit un fichier dans sa galerie ou son explorateur ; il
 * doit se retrouver hébergé sur Cloudinary et son lien enregistré en base, sans
 * qu'il ait à passer par la console Cloudinary.
 */
test.use({ storageState: 'e2e/.auth/directeur.json', viewport: { width: 1440, height: 900 } });

// Une vraie photo du projet : le test éprouve un fichier représentatif, pas un
// pixel factice que Cloudinary pourrait traiter différemment.
const PHOTO = 'public/media/photos/salon-lounge-sm.jpg';
const API = 'http://localhost:8080/api';
const MARQUE = 'ZZ-test-televersement';

/**
 * Jeton repris de la session enregistrée par `auth.setup`, sans nouvelle
 * connexion : le backend limite les tentatives (10 par tranche de 5 minutes) et
 * un appel à `/auth/login` dans chaque hook épuiserait ce quota, faisant échouer
 * toute la suite.
 */
function jetonDirecteur(): string {
  const etat = JSON.parse(readFileSync('e2e/.auth/directeur.json', 'utf8'));
  for (const origine of etat.origins ?? []) {
    const t = (origine.localStorage ?? []).find((e: any) => e.name === 'token');
    if (t) return t.value;
  }
  throw new Error("Jeton introuvable dans l'état de session enregistré.");
}

/**
 * Retire toute trace laissée par une exécution précédente. Passe par l'API : un
 * nettoyage par l'interface échouerait si le test s'est interrompu avant, et les
 * lignes s'accumuleraient dans la galerie réelle d'une exécution à l'autre.
 */
async function nettoyer(request: any) {
  const entetes = { Authorization: `Bearer ${jetonDirecteur()}` };
  const liste = await (await request.get(`${API}/medias/admin`, { headers: entetes })).json();
  for (const m of liste.filter((x: any) => x.titre.includes(MARQUE))) {
    await request.delete(`${API}/medias/${m.id}`, { headers: entetes });
  }
}

test.beforeEach(async ({ request }) => nettoyer(request));
test.afterEach(async ({ request }) => nettoyer(request));

test('le sélecteur de fichier est proposé, sans configuration serveur', async ({ page }) => {
  await page.goto('/administration/medias');
  await page.waitForTimeout(2000);

  // La zone de dépôt est l'action principale de la page.
  await expect(page.locator('.m-drop')).toBeVisible();
  await expect(page.getByText('Choisir une photo')).toBeVisible();
  // Le message « envoi indisponible » ne doit plus apparaître.
  await expect(page.locator('.m-notice')).toHaveCount(0);
});

test('choisir une photo affiche l’aperçu et pré-remplit le titre', async ({ page }) => {
  test.skip(!existsSync(PHOTO), 'photo de test absente');
  await page.goto('/administration/medias');
  await page.waitForTimeout(2000);

  await page.locator('.m-drop input[type=file]').setInputFiles(PHOTO);
  await page.waitForTimeout(900);

  await expect(page.locator('.m-preview')).toBeVisible();
  // Le titre part du nom du fichier : l'administrateur n'a rien à retaper.
  const titre = await page.locator('.m-fields input.gn-input').inputValue();
  expect(titre.length).toBeGreaterThan(0);
});

test('envoi complet : le fichier part sur Cloudinary et son lien est enregistré', async ({ page }) => {
  test.skip(!existsSync(PHOTO), 'photo de test absente');

  // On intercepte l'appel à Cloudinary : le test doit pouvoir tourner hors ligne
  // et ne pas déposer un fichier de plus sur le compte réel à chaque exécution.
  let recuParCloudinary: { preset?: string; publicId?: string } | null = null;
  await page.route('https://api.cloudinary.com/**', async route => {
    const corps = route.request().postData() || '';
    recuParCloudinary = {
      preset: /name="upload_preset"\r?\n\r?\n([^\r\n]+)/.exec(corps)?.[1],
      publicId: /name="public_id"\r?\n\r?\n([^\r\n]+)/.exec(corps)?.[1]
    };
    await route.fulfill({
      status: 200, contentType: 'application/json',
      body: JSON.stringify({ public_id: recuParCloudinary.publicId, bytes: 1234 })
    });
  });

  let enregistre: any = null;
  await page.route('**/api/medias/televersement', async route => {
    enregistre = JSON.parse(route.request().postData() || '{}');
    await route.continue();
  });

  await page.goto('/administration/medias');
  await page.waitForTimeout(2000);
  const avant = await page.locator('.m-card').count();

  await page.locator('.m-drop input[type=file]').setInputFiles(PHOTO);
  await page.waitForTimeout(900);
  await page.locator('.m-fields input.gn-input').fill(MARQUE);
  await page.getByRole('button', { name: /Publier la photo/ }).click();

  await expect(page.locator('.m-card')).toHaveCount(avant + 1, { timeout: 20000 });

  // Le fichier est bien parti chez Cloudinary avec le bon préréglage…
  expect(recuParCloudinary).not.toBeNull();
  expect(recuParCloudinary!.preset).toBe('gnimah_media');
  // …dans le dossier de la galerie, ce que le serveur exige.
  expect(recuParCloudinary!.publicId).toMatch(/^gnimah\/galerie\//);

  // …et c'est bien un lien Cloudinary qui a été enregistré, pas le fichier.
  expect(enregistre.url).toContain('res.cloudinary.com/dehzeeo6p');
  expect(enregistre.publicId).toBe(recuParCloudinary!.publicId);
  expect(enregistre.titre).toBe(MARQUE);

  // La suppression depuis l'interface fonctionne aussi (le `afterEach` couvre
  // le nettoyage si ce passage échoue).
  await page.locator('.m-card', { hasText: MARQUE }).first().locator('.m-danger').click();
  await page.locator('.p-confirmdialog').getByRole('button', { name: 'Supprimer' }).click();
  await expect(page.locator('.m-card', { hasText: MARQUE })).toHaveCount(0, { timeout: 15000 });
});

test('le serveur refuse un fichier hors du dossier de la galerie', async ({ page }) => {
  await page.goto('/administration/medias');
  await page.waitForTimeout(1500);

  // Un client qui ferait adopter un objet quelconque du compte pourrait ensuite
  // le faire effacer en retirant la ligne de la galerie.
  const rep = await page.evaluate(async () => {
    const jeton = localStorage.getItem('token') || '';
    // URL absolue : en développement le serveur Angular ne relaie pas /api et
    // renverrait sa page d'accueil au lieu de la réponse de l'API.
    const r = await fetch('http://localhost:8080/api/medias/televersement', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${jeton}` },
      body: JSON.stringify({
        titre: 'Intrus', categorie: 'Chambres',
        url: 'https://res.cloudinary.com/dehzeeo6p/image/upload/gnimah/photos/facade-patio',
        publicId: 'gnimah/photos/facade-patio'
      })
    });
    return { statut: r.status, corps: await r.text() };
  });

  expect(rep.statut).toBeGreaterThanOrEqual(400);
  expect(rep.corps).toContain('galerie');
});
