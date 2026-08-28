/**
 * Jetons de couleur des graphiques GNIMAH.
 *
 * Chaque valeur ci-dessous a été validée avec le validateur de palette
 * (bande de clarté OKLCH, plancher de chroma, séparation daltonisme protan/deutan
 * ΔE OKLab×100, plancher vision normale, contraste sur la surface blanche des
 * cartes `.gn-card`). Ne pas remplacer une teinte « à l'œil » : toute nouvelle
 * couleur doit repasser la validation.
 *
 *   Catégoriel (paires adjacentes) — pire ΔE daltonisme 16,5 · vision normale 22,7
 *   États de chambre (toutes paires) — pire ΔE daltonisme 8,2 · vision normale 17,0
 *   Rampe ordinale bordeaux — clarté monotone, extrémité claire à 2,85:1
 */

/** Ordre fixe des teintes de série. On assigne dans l'ordre, jamais en boucle. */
export const SERIES = [
  '#8c2f4d', // 1 · bordeaux (teinte de marque)
  '#ab8529', // 2 · or
  '#4a6fb5', // 3 · ardoise
  '#b25a34', // 4 · terre cuite
  '#6c4ba8', // 5 · prune
  '#12855f'  // 6 · vert
] as const;

/**
 * États de chambre — validé « toutes paires » car la légende met les quatre
 * couleurs côte à côte, et non seulement les segments voisins.
 */
export const ETAT_SERIES: Record<string, string> = {
  LIBRE: '#12855f',
  OCCUPEE: '#8c2f4d',
  A_NETTOYER: '#ab8529',
  EN_MAINTENANCE: '#4a6fb5',
  HORS_SERVICE: '#9a8a86'
};

/** Rampe ordinale bordeaux : clair → foncé, pour les étapes de l'entonnoir. */
export const RAMPE_BORDEAUX = ['#cf8399', '#a94566', '#7f2542'] as const;

/** Habillage : grilles, axes et encres. Toujours discret face aux données. */
export const CHROME = {
  surface: '#ffffff',
  grille: '#f2e8e4',
  axe: '#e0d0cb',
  encre: '#2e2024',
  encreSecondaire: '#6d585d',
  encreDiscrete: '#a99a9d'
} as const;

/** Formats francophones partagés par les axes, infobulles et étiquettes. */
export const MOIS_COURTS = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];

/** « 2026-08-14 » → « 14/08 » : format compact pour l'axe des abscisses. */
export function labelJourCourt(iso: string): string {
  const [, m, j] = (iso || '').split('-');
  return j && m ? `${j}/${m}` : iso;
}

/** « 2026-08-14 » → « jeudi 14 août » : libellé long pour les infobulles. */
export function labelJourLong(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  if (isNaN(d.getTime())) return iso;
  const jours = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];
  return `${jours[d.getDay()]} ${d.getDate()} ${MOIS_COURTS[d.getMonth()]}`;
}

/**
 * Montants compacts pour les graduations : « 0 », « 45 k », « 1,2 M ».
 * Les axes portent les valeurs que l'on n'étiquette pas directement, donc ils
 * doivent rester lisibles sans occuper toute la largeur.
 */
export function montantCompact(v: number): string {
  const n = Math.abs(v || 0);
  if (n >= 1_000_000) return `${(v / 1_000_000).toFixed(n >= 10_000_000 ? 0 : 1).replace('.', ',')} M`;
  if (n >= 1_000) return `${Math.round(v / 1_000)} k`;
  return `${Math.round(v || 0)}`;
}

/** Montant complet en FCFA, séparateurs francophones : « 145 000 FCFA ». */
export function montantComplet(v: number): string {
  return `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(v || 0)} FCFA`;
}
