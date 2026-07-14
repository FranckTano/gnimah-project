export interface UtilisateurResponse {
  id: number;
  username: string;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  actif: boolean;
  dateCreation: string;
}

export interface UtilisateurRequest {
  username: string;
  password?: string;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  actif: boolean;
}

export const roleLabel: Record<string, string> = {
  ADMIN: 'Administrateur',
  DIRECTEUR: 'Directeur',
  AGENT: 'Agent de réception'
};
