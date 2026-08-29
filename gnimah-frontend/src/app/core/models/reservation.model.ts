export type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'ARRIVEE' | 'ANNULEE' | 'NO_SHOW';

export interface ReservationRequest {
  clientId: number;
  chambreId?: number;
  typeChambre?: string;
  dateArrivee: string;
  dateDepart: string;
  /** Tarif de nuitée négocié ; vide = tarif catalogue. */
  tarifPersonnalise?: number | null;
  remiseType?: string | null;
  remiseValeur?: number | null;
  remiseMotif?: string | null;
  /** Acompte exigé pour confirmer ; vide = 30 % du montant prévu. */
  acompteRequis?: number | null;
  /** Acompte encaissé immédiatement — confirme la réservation s'il suffit. */
  acompte?: number | null;
  modeAcompte?: string;
  referenceAcompte?: string;
  caisseId?: number | null;
  notes?: string;
}

export interface AcompteRequest {
  montant: number;
  mode: string;
  referenceTransaction?: string;
  caisseId?: number | null;
  notes?: string;
}

export interface AnnulationRequest {
  motif?: string;
  rembourser?: boolean;
  caisseId?: number | null;
  mode?: string;
}

export interface PolitiqueAnnulation {
  reservationId: number;
  dateArrivee: string;
  dateLimiteAnnulation: string;
  heuresAvantArrivee: number;
  dansLesDelais: boolean;
  acompteVerse: number;
  penalite: number;
  remboursement: number;
  message: string;
}

export interface ReservationResponse {
  id: number;
  numeroReservation: string;
  clientId: number;
  clientNom: string;
  clientTelephone: string;
  chambreId: number;
  chambreNumero: string;
  typeChambre: string;
  dateArrivee: string;
  dateDepart: string;
  nbNuits: number;

  montantBrut: number;
  remiseType: string | null;
  remiseValeur: number;
  remiseMontant: number;
  remiseMotif: string | null;
  montantPrevu: number;

  acompte: number;
  acompteRequis: number;
  resteAcompte: number;
  dateAcompte: string | null;
  modeAcompte: string | null;
  referenceAcompte: string | null;
  acompteSuffisant: boolean;

  dateLimiteAnnulation: string;
  annulableSansFrais: boolean;
  dateAnnulation: string | null;
  motifAnnulation: string | null;
  penaliteAnnulation: number;
  montantRembourse: number;
  annuleParNom: string | null;

  statut: StatutReservation;
  notes: string;
  createdAt: string;
}

export const STATUT_RESERVATION_LABELS: Record<StatutReservation, string> = {
  EN_ATTENTE: 'En attente d\'acompte',
  CONFIRMEE: 'Confirmée',
  ARRIVEE: 'Arrivée',
  ANNULEE: 'Annulée',
  NO_SHOW: 'Non présenté'
};

/** Préavis d'annulation sans frais — doit rester aligné sur la règle du backend. */
export const PREAVIS_ANNULATION_HEURES = 48;
