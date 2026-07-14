export interface TacheEntretienResponse {
  id: number;
  chambreNumero: string;
  chambreId: number;
  type: string;
  description: string;
  statut: string;
  assigneA: string | null;
  dateCreation: string;
  dateFin: string | null;
  priorite: string;
}

export interface TacheEntretienRequest {
  chambreId: number;
  type: string;
  description: string;
  assigneA?: string;
  priorite?: string;
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
