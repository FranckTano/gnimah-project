/**
 * Période d'analyse partagée par le tableau de bord et la page Statistiques.
 *
 * Les deux écrans doivent proposer exactement les mêmes raccourcis et la même
 * lecture des dates : la logique vit donc ici, pas dans les composants.
 */

export type Preset = 'jour' | 'semaine' | 'mois' | 'mois-courant' | 'perso';

export interface Periode {
  /** Date ISO « AAAA-MM-JJ » incluse. */
  debut: string;
  /** Date ISO « AAAA-MM-JJ » incluse. */
  fin: string;
  preset: Preset;
}

export const PRESETS: { cle: Exclude<Preset, 'perso'>; label: string }[] = [
  { cle: 'jour', label: "Aujourd'hui" },
  { cle: 'semaine', label: '7 derniers jours' },
  { cle: 'mois', label: '30 derniers jours' },
  { cle: 'mois-courant', label: 'Mois en cours' }
];

/** Date locale au format « AAAA-MM-JJ » (jamais `toISOString`, qui bascule en UTC). */
export function iso(d: Date): string {
  const p = (n: number) => `${n}`.padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function periodeDepuisPreset(preset: Preset, aujourdhui = new Date()): Periode {
  const fin = iso(aujourdhui);

  if (preset === 'semaine' || preset === 'mois') {
    const debut = new Date(aujourdhui);
    debut.setDate(debut.getDate() - (preset === 'semaine' ? 6 : 29));
    return { debut: iso(debut), fin, preset };
  }
  if (preset === 'mois-courant') {
    return { debut: iso(new Date(aujourdhui.getFullYear(), aujourdhui.getMonth(), 1)), fin, preset };
  }
  // 'jour' et 'perso' retombent sur la journée en cours.
  return { debut: fin, fin, preset };
}

const JOURS = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];
const MOIS = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];

/** « 2026-08-14 » → « jeudi 14 août ». */
export function labelJourLong(isoDate: string): string {
  const d = new Date(`${isoDate}T00:00:00`);
  if (isNaN(d.getTime())) return isoDate;
  return `${JOURS[d.getDay()]} ${d.getDate()} ${MOIS[d.getMonth()]}`;
}

export function libellePeriode(p: Periode): string {
  if (!p.debut && !p.fin) return 'Toute la période';
  if (p.debut === p.fin) return labelJourLong(p.debut);
  return `${labelJourLong(p.debut)} → ${labelJourLong(p.fin)}`;
}

export function nbJours(p: Periode): number {
  if (!p.debut || !p.fin) return 1;
  const d1 = new Date(`${p.debut}T00:00:00`).getTime();
  const d2 = new Date(`${p.fin}T00:00:00`).getTime();
  return Math.max(1, Math.round((d2 - d1) / 86_400_000) + 1);
}
