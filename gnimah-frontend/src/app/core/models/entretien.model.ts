export interface TacheEntretienResponse {
  id: number;
  titre: string;
  chambreId: number | null;
  chambreNumero: string | null;
  salle: string | null;
  evenementId: number | null;
  evenementTitre: string | null;
  type: string;
  description: string;
  statut: string;
  priorite: string;
  agentId: number | null;
  assigneA: string | null;
  responsableId: number | null;
  responsableNom: string | null;
  dateLimite: string | null;
  dateCreation: string;
  dateFin: string | null;
}

export interface TacheEntretienRequest {
  titre: string;
  type: string;
  description: string;
  chambreId?: number | null;
  salle?: string | null;
  evenementId?: number | null;
  agentId: number;
  priorite?: string;
  dateLimite?: string | null;
}

export const statutTacheLabel: Record<string, string> = {
  A_FAIRE: 'En attente',
  EN_COURS: 'En cours',
  TERMINE: 'Terminée',
  ANNULE: 'Annulée'
};

export const statutTacheColor: Record<string, string> = {
  A_FAIRE: 'warning',
  EN_COURS: 'info',
  TERMINE: 'success',
  ANNULE: 'danger'
};
