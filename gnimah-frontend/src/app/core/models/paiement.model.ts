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
  referenceTransaction?: string;
  notes?: string;
}

export const modePaiementLabel: Record<string, string> = {
  ESPECES:       'Espèces',
  CARTE_BANCAIRE:'Carte bancaire',
  WAVE:          'Wave',
  ORANGE_MONEY:  'Orange Money',
  MTN_MONEY:     'MTN Mobile Money',
  MOOV_MONEY:    'Moov Money',
  MOBILE_MONEY:  'Mobile Money',
  CHEQUE:        'Chèque',
  VIREMENT:      'Virement bancaire',
  AUTRE:         'Autre'
};

export const modePaiementOptions = [
  { label: 'Espèces',           value: 'ESPECES' },
  { label: 'Wave',              value: 'WAVE' },
  { label: 'Orange Money',      value: 'ORANGE_MONEY' },
  { label: 'MTN Mobile Money',  value: 'MTN_MONEY' },
  { label: 'Moov Money',        value: 'MOOV_MONEY' },
  { label: 'Carte bancaire',    value: 'CARTE_BANCAIRE' },
  { label: 'Mobile Money',      value: 'MOBILE_MONEY' },
  { label: 'Chèque',            value: 'CHEQUE' },
  { label: 'Virement bancaire', value: 'VIREMENT' },
  { label: 'Autre',             value: 'AUTRE' }
];
