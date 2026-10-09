export type TypeLocation = 'PASSAGE' | 'SEJOUR';

export interface SejourRequest {
  clientId: number;
  chambreId: number;
  typeLocation: TypeLocation;
  dateEntree: string;
  dateSortie?: string;
  heureEntree?: string;
  heureSortie?: string;
  /** Tarif unitaire négocié ; vide = tarif catalogue de la chambre. */
  tarifPersonnalise?: number | null;
  remiseType?: string | null;
  remiseValeur?: number | null;
  remiseMotif?: string | null;
  montantPaye?: number;
  modePaiement?: string;
  notes?: string;
  reservationId?: number;
  caisseId?: number | null;
}

export interface RemiseSejourRequest {
  tarifPersonnalise?: number | null;
  remiseType?: string | null;
  remiseValeur?: number | null;
  remiseMotif?: string | null;
  /** Nouvelle date de départ « AAAA-MM-JJ » (prolongation) ; vide = durée inchangée. */
  dateSortie?: string | null;
}

export interface SejourResponse {
  id: number;
  numeroRecu: string;
  clientId: number;
  clientNom: string;
  clientTelephone: string;
  chambreId: number;
  chambreNumero: string;
  chambreType: string;
  agentNom: string;
  typeLocation: TypeLocation;
  dateEntree: string;
  dateSortie: string;
  heureEntree: string;
  heureSortie: string;
  nbJours: number;
  nbHeures: number;

  montantBrut: number;
  tarifApplique: number | null;
  remiseType: string | null;
  remiseValeur: number;
  remiseMontant: number;
  remiseMotif: string | null;
  remiseParNom: string | null;
  montantExtras: number;

  montantTotal: number;
  montantPaye: number;
  resteAPayer: number;
  statut: 'EN_COURS' | 'TERMINE' | 'ANNULE';
  notes: string;
  createdAt: string;
}

export const TYPE_REMISE_OPTIONS = [
  { label: 'Pourcentage (%)', value: 'POURCENTAGE' },
  { label: 'Montant (FCFA)',  value: 'MONTANT' }
];
