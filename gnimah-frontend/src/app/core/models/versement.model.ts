export type StatutVersement = 'PREPARE' | 'EFFECTUE' | 'CONFIRME' | 'ANNULE';

export interface ActionVersementResponse {
  id: number;
  action: string;
  dateAction: string;
  montant: number | null;
  agentNom: string | null;
  commentaire: string | null;
}

export interface VersementResponse {
  id: number;
  numero: string;
  caisseId: number;
  caisseNom: string;
  compteId: number;
  compteLibelle: string;
  compteType: string;
  compteIdentifiant: string | null;
  periodicite: string;
  periodeDebut: string;
  periodeFin: string;
  montant: number;
  dateVersement: string;
  heureVersement: string | null;
  mode: string;
  referenceTransaction: string | null;
  statut: StatutVersement;
  caissierNom: string | null;
  valideParNom: string | null;
  dateValidation: string | null;
  justificatifUrl: string | null;
  notes: string | null;
  createdAt: string;
  actions: ActionVersementResponse[];
}

export interface VersementRequest {
  caisseId: number;
  compteId: number;
  periodicite?: string;
  periodeDebut?: string;
  periodeFin?: string;
  montant: number;
  dateVersement: string;
  heureVersement?: string;
  mode?: string;
  referenceTransaction?: string;
  justificatifUrl?: string;
  notes?: string;
}

export interface ActionVersementRequest {
  montant?: number;
  referenceTransaction?: string;
  commentaire?: string;
}

export interface RecetteAVerserResponse {
  caisseId: number;
  caisseNom: string;
  periodeDebut: string;
  periodeFin: string;
  recetteEncaissee: number;
  dejaVerse: number;
  resteAVerser: number;
  soldeCaisse: number;
}

export interface CompteFinancierResponse {
  id: number;
  libelle: string;
  type: 'BANQUE' | 'MOBILE_MONEY';
  etablissement: string | null;
  numeroCompte: string | null;
  numeroTelephone: string | null;
  identifiant: string | null;
  titulaire: string | null;
  solde: number;
  actif: boolean;
  notes: string | null;
}

export interface CompteFinancierRequest {
  libelle: string;
  type?: string;
  etablissement?: string;
  numeroCompte?: string;
  numeroTelephone?: string;
  titulaire?: string;
  notes?: string;
  actif?: boolean;
}

export const PERIODICITE_OPTIONS = [
  { label: 'Journalier',   value: 'JOURNALIER' },
  { label: 'Hebdomadaire', value: 'HEBDOMADAIRE' },
  { label: 'Mensuel',      value: 'MENSUEL' },
  { label: 'Ponctuel',     value: 'PONCTUEL' }
];

export const PERIODICITE_LABELS: Record<string, string> = {
  JOURNALIER: 'Journalier',
  HEBDOMADAIRE: 'Hebdomadaire',
  MENSUEL: 'Mensuel',
  PONCTUEL: 'Ponctuel'
};

export const STATUT_VERSEMENT_LABELS: Record<string, string> = {
  PREPARE:  'Préparé',
  EFFECTUE: 'Déposé',
  CONFIRME: 'Confirmé',
  ANNULE:   'Annulé'
};

export const ACTION_VERSEMENT_LABELS: Record<string, string> = {
  PREPARATION:  'Préparation',
  DEPOT:        'Dépôt effectué',
  CONFIRMATION: 'Confirmation',
  REJET:        'Rejet',
  ANNULATION:   'Annulation',
  MODIFICATION: 'Modification'
};

export const ACTION_VERSEMENT_ICONS: Record<string, string> = {
  PREPARATION:  'ti-file-text',
  DEPOT:        'ti-building-bank',
  CONFIRMATION: 'ti-circle-check',
  REJET:        'ti-alert-triangle',
  ANNULATION:   'ti-x',
  MODIFICATION: 'ti-pencil'
};

export const TYPE_COMPTE_OPTIONS = [
  { label: 'Compte bancaire', value: 'BANQUE' },
  { label: 'Mobile Money',    value: 'MOBILE_MONEY' }
];
