import { test, expect, APIRequestContext } from '@playwright/test';
import { readFileSync } from 'fs';

/**
 * Parcours de réception ajoutés pour la mise en production, et affichage des
 * montants en franc CFA (« FCFA », jamais le code « XOF »).
 *
 * Les données nécessaires sont créées par l'API avec le jeton déjà obtenu par
 * auth.setup.ts : aucune connexion supplémentaire, le backend limitant les
 * tentatives de connexion répétées.
 */
const API = 'http://localhost:8080/api';

function jetonDe(role: string): string {
  const etat = JSON.parse(readFileSync(`e2e/.auth/${role}.json`, 'utf-8'));
  const origine = etat.origins.find((o: any) => o.origin === 'http://localhost:4200');
  return origine.localStorage.find((e: any) => e.name === 'token').value;
}

async function appel(api: APIRequestContext, jeton: string, methode: 'GET' | 'POST', chemin: string, corps?: unknown) {
  const reponse = await api.fetch(`${API}${chemin}`, {
    method: methode,
    headers: { Authorization: `Bearer ${jeton}`, 'Content-Type': 'application/json' },
    data: corps ? JSON.stringify(corps) : undefined
  });
  expect(reponse.ok(), `${methode} ${chemin} → ${reponse.status()} ${await reponse.text()}`).toBeTruthy();
  return reponse.json();
}

function iso(d: Date): string {
  const p = (n: number) => `${n}`.padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

async function nouveauClient(api: APIRequestContext, jeton: string, nom: string) {
  const suffixe = `${Date.now()}`.slice(-8);
  return appel(api, jeton, 'POST', '/clients', {
    civilite: 'M.', nom, prenom: 'Playwright', telephone: `+225 07 ${suffixe}`,
    typePiece: 'CNI', numeroPiece: `PW-${suffixe}`
  });
}

async function chambreLibre(api: APIRequestContext, jeton: string) {
  const chambres = await appel(api, jeton, 'GET', '/chambres/disponibles');
  expect(chambres.length, 'au moins une chambre libre').toBeGreaterThan(0);
  return chambres[0];
}

test.describe('Réception — encaissement, arrivée sur réservation et devise', () => {
  test.use({ storageState: 'e2e/.auth/agent.json' });

  test("encaisse le solde d'un séjour depuis la liste et propose le reçu", async ({ page, request }) => {
    const jeton = jetonDe('agent');
    const client = await nouveauClient(request, jeton, `Solde${Date.now()}`);
    const chambre = await chambreLibre(request, jeton);
    const aujourd = new Date();
    const demain = new Date(aujourd.getTime() + 86_400_000);
    const sejour = await appel(request, jeton, 'POST', '/sejours/check-in', {
      clientId: client.id, chambreId: chambre.id, typeLocation: 'SEJOUR',
      dateEntree: iso(aujourd), dateSortie: iso(demain), montantPaye: 0
    });
    expect(sejour.resteAPayer).toBeGreaterThan(0);

    await page.goto('/sejours');
    const ligne = page.locator('tr', { hasText: client.nomComplet });
    await expect(ligne).toBeVisible();
    await expect(ligne).toContainText('FCFA');
    await ligne.getByRole('button', { name: 'Encaisser' }).click();

    const dialogue = page.getByRole('dialog');
    await expect(dialogue).toContainText('Reste à payer');
    await expect(dialogue.locator('[formcontrolname="montant"]')).toHaveValue(`${sejour.resteAPayer}`);
    await dialogue.getByRole('button', { name: 'Encaisser', exact: true }).click();

    await expect(dialogue).toContainText('encaissés');
    await expect(dialogue.getByRole('button', { name: /Télécharger le reçu/ })).toBeVisible();
    expect(await dialogue.innerText()).not.toContain('XOF');
  });

  test("ouvre l'arrivée pré-remplie depuis une réservation et déduit l'acompte", async ({ page, request }) => {
    const jeton = jetonDe('agent');
    const client = await nouveauClient(request, jeton, `Resa${Date.now()}`);
    const chambre = await chambreLibre(request, jeton);
    const aujourd = new Date();
    const depart = new Date(aujourd.getTime() + 2 * 86_400_000);
    const reservation = await appel(request, jeton, 'POST', '/reservations', {
      clientId: client.id, chambreId: chambre.id, dateArrivee: iso(aujourd), dateDepart: iso(depart)
    });
    const confirmee = await appel(request, jeton, 'POST', `/reservations/${reservation.id}/acompte`, {
      montant: reservation.acompteRequis, mode: 'ESPECES'
    });
    expect(confirmee.statut).toBe('CONFIRMEE');

    await page.goto('/reservations');
    const ligne = page.locator('tr', { hasText: client.nomComplet });
    await ligne.locator('button[title="Enregistrer l\'arrivée (séjour)"]').click();

    await expect(page).toHaveURL(new RegExp(`/sejours/check-in\\?reservation=${reservation.id}`));
    await expect(page.getByText(`Arrivée de la réservation ${reservation.numeroReservation}`)).toBeVisible();
    await expect(page.locator('.gn-clientfound')).toContainText(client.nom);
    await expect(page.locator('.gn-chip.on')).toContainText(chambre.numero);
    await expect(page.getByText('Acompte versé à la réservation')).toBeVisible();

    await page.getByRole('button', { name: /Valider & générer le reçu/ }).click();
    await expect(page.getByRole('main').getByText('Séjour enregistré')).toBeVisible();

    const apres = await appel(request, jeton, 'GET', `/reservations/${reservation.id}`);
    expect(apres.statut).toBe('ARRIVEE');
  });
});

test.describe('Affichage des montants : FCFA partout, jamais XOF', () => {
  test.use({ storageState: 'e2e/.auth/directeur.json' });

  const ecrans = [
    '/dashboard', '/kpi', '/comptabilite', '/comptabilite/depenses', '/caisse', '/caisse/versements',
    '/sejours', '/reservations', '/paiements', '/restauration', '/stock', '/chambres', '/chambres/liste',
    '/entretien/incidents', '/sejours/check-in', '/evenements'
  ];

  for (const ecran of ecrans) {
    test(`${ecran}`, async ({ page }) => {
      await page.goto(ecran);
      await page.waitForLoadState('networkidle');
      const texte = await page.locator('body').innerText();
      expect(texte, `« XOF » affiché sur ${ecran}`).not.toContain('XOF');
      if (!['/chambres', '/evenements', '/sejours/check-in'].includes(ecran)) {
        expect(texte, `aucun montant en FCFA sur ${ecran}`).toContain('FCFA');
      }
    });
  }
});
