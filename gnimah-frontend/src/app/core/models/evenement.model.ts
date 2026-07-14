export interface EvenementResponse {
  id: number;
  titre: string;
  description: string;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  statut: string;
  nombreParticipants: number;
  montant: number;
  clientNom: string | null;
}

export interface EvenementRequest {
  titre: string;
  description: string;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  nombreParticipants?: number;
  montant?: number;
  clientId?: number;
}
