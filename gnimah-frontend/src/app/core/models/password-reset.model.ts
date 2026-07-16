export interface PasswordResetRequestResponse {
  id: number;
  utilisateurId: number;
  utilisateurNomComplet: string;
  username: string;
  statut: string;
  createdAt: string;
}

export interface ResoudreReinitialisationResponse {
  username: string;
  nouveauMotDePasse: string;
}
