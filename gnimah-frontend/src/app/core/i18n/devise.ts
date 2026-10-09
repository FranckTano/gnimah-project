import { CurrencyPipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

/**
 * Affichage des montants : le franc CFA, écrit « FCFA », à la française
 * (« 15 000 FCFA »).
 *
 * Tous les montants passent par le pipe `currency` d'Angular : directement dans
 * les templates (`{{ montant | currency }}`), et via {@link formatMontant} là où
 * le texte se compose en TypeScript (notifications, confirmations, infobulles
 * des graphiques). Un seul réglage, donc un seul rendu sur tous les écrans — et
 * qui ne dépend plus des données de langue installées dans le navigateur.
 */
export const LOCALE_APP = 'fr';

/**
 * Code ISO 4217 du franc CFA de l'UEMOA. Il n'est jamais affiché : le pipe s'en
 * sert pour savoir que la devise n'a pas de centimes.
 */
export const CODE_DEVISE = 'XOF';

/** Symbole affiché après les montants. */
export const SYMBOLE_DEVISE = 'FCFA';

/**
 * Enregistre la locale française avec « FCFA » comme symbole du franc CFA
 * (Angular écrit « F CFA » par défaut). Si la structure des données de locale
 * venait à changer, la locale d'origine est enregistrée telle quelle : le
 * franc CFA s'afficherait alors « F CFA », jamais sous son code ISO.
 */
function enregistrerLocaleFrancaise(): void {
  const donnees = [...(localeFr as unknown[])];
  const iDevises = donnees.findIndex(d =>
    !!d && typeof d === 'object' && !Array.isArray(d) && 'USD' in (d as object));
  if (iDevises >= 0) {
    donnees[iDevises] = { ...(donnees[iDevises] as object), [CODE_DEVISE]: [SYMBOLE_DEVISE, SYMBOLE_DEVISE] };
  }
  registerLocaleData(donnees, LOCALE_APP);
}

enregistrerLocaleFrancaise();

const pipeMontant = new CurrencyPipe(LOCALE_APP, CODE_DEVISE);

/**
 * « 15 000 FCFA » — rendu identique à `{{ montant | currency }}`, pour les
 * textes composés en TypeScript. Une valeur absente s'affiche « 0 FCFA ».
 */
export function formatMontant(valeur: number | string | null | undefined): string {
  return pipeMontant.transform(Number(valeur) || 0) ?? '';
}
