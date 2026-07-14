export type TypeLocation = 'PASSAGE' | 'SEJOUR';

export interface SejourRequest {
  clientId: number;
  chambreId: number;
  typeLocation: TypeLocation;
  dateEntree: string;
  dateSortie?: string;
  heureEntree?: string;
  heureSortie?: string;
  montantPaye?: number;
  notes?: string;
  reservationId?: number;
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
  montantTotal: number;
  montantPaye: number;
  resteAPayer: number;
  statut: 'EN_COURS' | 'TERMINE' | 'ANNULE';
  notes: string;
  createdAt: string;
}
