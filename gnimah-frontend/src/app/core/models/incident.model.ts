export type StatutIncident =
  'DECLARE' | 'EN_COURS' | 'REPARE' | 'REMPLACE' | 'IMPUTE_CLIENT' | 'CLASSE_SANS_SUITE';

export interface IncidentResponse {
  id: number;
  numero: string;
  chambreId: number;
  chambreNumero: string;
  sejourId: number | null;
  sejourNumeroRecu: string | null;
  clientId: number | null;
  clientNom: string | null;
  type: string;
  designation: string;
  articleStockId: number | null;
  articleDesignation: string | null;
  quantite: number;
  gravite: 'MINEUR' | 'MOYEN' | 'MAJEUR';
  description: string | null;
  photo: string | null;
  statut: StatutIncident;
  coutEstime: number;
  coutReel: number;
  imputeClient: boolean;
  declareParNom: string | null;
  resoluParNom: string | null;
  tacheId: number | null;
  depenseId: number | null;
  dateIncident: string;
  dateResolution: string | null;
  notes: string | null;
}

export interface IncidentRequest {
  chambreId: number;
  sejourId?: number | null;
  type: string;
  designation: string;
  articleStockId?: number | null;
  quantite?: number;
  gravite?: string;
  description?: string;
  photo?: string;
  coutEstime?: number;
  imputeClient?: boolean;
  bloquerChambre?: boolean;
  notes?: string;
}

export interface ResolutionIncidentRequest {
  statut: string;
  coutReel?: number;
  imputeClient?: boolean;
  familleDepense?: string;
  caisseId?: number | null;
  notes?: string;
  libererChambre?: boolean;
}

export const TYPE_INCIDENT_OPTIONS = [
  { label: 'Objet manquant',      value: 'OBJET_MANQUANT' },
  { label: 'Objet cassé',         value: 'OBJET_CASSE' },
  { label: "Panne d'équipement",  value: 'PANNE_EQUIPEMENT' },
  { label: 'Dégât sur mobilier',  value: 'DEGAT_MOBILIER' },
  { label: 'Dégât sanitaire',     value: 'DEGAT_SANITAIRE' },
  { label: 'Linge abîmé',         value: 'LINGE_ABIME' },
  { label: 'Propreté',            value: 'PROPRETE' },
  { label: 'Autre',               value: 'AUTRE' }
];

export const TYPE_INCIDENT_LABELS: Record<string, string> =
  TYPE_INCIDENT_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

export const TYPE_INCIDENT_ICONS: Record<string, string> = {
  OBJET_MANQUANT:   'ti-help-circle',
  OBJET_CASSE:      'ti-alert-triangle',
  PANNE_EQUIPEMENT: 'ti-plug-off',
  DEGAT_MOBILIER:   'ti-armchair',
  DEGAT_SANITAIRE:  'ti-bath',
  LINGE_ABIME:      'ti-wash',
  PROPRETE:         'ti-spray',
  AUTRE:            'ti-dots'
};

export const GRAVITE_OPTIONS = [
  { label: 'Mineur', value: 'MINEUR' },
  { label: 'Moyen',  value: 'MOYEN' },
  { label: 'Majeur', value: 'MAJEUR' }
];

export const GRAVITE_LABELS: Record<string, string> = {
  MINEUR: 'Mineur',
  MOYEN:  'Moyen',
  MAJEUR: 'Majeur'
};

export const GRAVITE_TONES: Record<string, string> = {
  MINEUR: 'slate',
  MOYEN:  'amber',
  MAJEUR: 'red'
};

export const STATUT_INCIDENT_LABELS: Record<string, string> = {
  DECLARE:           'Déclaré',
  EN_COURS:          'En cours',
  REPARE:            'Réparé',
  REMPLACE:          'Remplacé',
  IMPUTE_CLIENT:     'Facturé au client',
  CLASSE_SANS_SUITE: 'Classé sans suite'
};

export const STATUT_INCIDENT_TONES: Record<string, string> = {
  DECLARE:           'amber',
  EN_COURS:          'slate',
  REPARE:            'green',
  REMPLACE:          'green',
  IMPUTE_CLIENT:     'wine',
  CLASSE_SANS_SUITE: 'gray'
};

/** Issues proposées à la clôture — « déclaré » et « en cours » n'en sont pas. */
export const RESOLUTION_OPTIONS = [
  { label: 'Réparé',            value: 'REPARE' },
  { label: 'Remplacé',          value: 'REMPLACE' },
  { label: 'Classé sans suite', value: 'CLASSE_SANS_SUITE' }
];
