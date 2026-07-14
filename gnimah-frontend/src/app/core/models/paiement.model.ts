export interface PaiementResponse {
  id: number;
  sejourId: number;
  clientNom: string;
  numeroChambre: string;
  montant: number;
  modePaiement: string;
  dateHeure: string;
  numeroRecu: string;
}

export interface PaiementRequest {
  sejourId: number;
  montant: number;
  mode: string;
}

export const modePaiementLabel: Record<string, string> = {
  ESPECES: 'Espèces',
  CARTE_BANCAIRE: 'Carte bancaire',
  VIREMENT: 'Virement',
  MOBILE_MONEY: 'Mobile Money',
  CHEQUE: 'Chèque'
};
