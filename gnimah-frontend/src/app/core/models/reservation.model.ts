export type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'ARRIVEE' | 'ANNULEE' | 'NO_SHOW';

export interface ReservationRequest {
  clientId: number;
  chambreId?: number;
  typeChambre?: string;
  dateArrivee: string;
  dateDepart: string;
  acompte?: number;
  notes?: string;
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
  montantPrevu: number;
  acompte: number;
  statut: StatutReservation;
  notes: string;
  createdAt: string;
}

export const STATUT_RESERVATION_LABELS: Record<StatutReservation, string> = {
  EN_ATTENTE: 'En attente',
  CONFIRMEE: 'Confirmée',
  ARRIVEE: 'Arrivée',
  ANNULEE: 'Annulée',
  NO_SHOW: 'No-show'
};
