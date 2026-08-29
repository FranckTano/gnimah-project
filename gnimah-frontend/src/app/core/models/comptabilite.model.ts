export interface LigneRepartition {
  cle: string;
  libelle: string;
  nombre: number;
  montant: number;
  part: number;
}

export interface SyntheseComptable {
  debut: string;
  fin: string;

  recetteHebergement: number;
  recetteRestauration: number;
  recetteEvenements: number;
  autresRecettes: number;
  totalRecettes: number;

  totalDepenses: number;
  depensesPayees: number;
  depensesEnAttente: number;

  resultat: number;
  tauxMarge: number;

  soldeCaisses: number;
  entreesCaisse: number;
  sortiesCaisse: number;
  totalVerse: number;
  resteAVerser: number;

  creancesClients: number;
  valeurStock: number;
  coutIncidents: number;

  depensesParFamille: LigneRepartition[];
  recettesParSource: LigneRepartition[];
  topFournisseurs: LigneRepartition[];
  evolutionJournaliere: { date: string; recettes: number; depenses: number; resultat: number }[];
}

export type StatutDepense = 'EN_ATTENTE' | 'VALIDEE' | 'PAYEE' | 'REJETEE';

export interface DepenseResponse {
  id: number;
  libelle: string;
  montant: number;
  famille: string;
  categorie: string;
  dateDepense: string;
  fournisseurId: number | null;
  fournisseurNom: string | null;
  modeReglement: string;
  referencePiece: string | null;
  statut: StatutDepense;
  caisseId: number | null;
  caisseNom: string | null;
  chambreId: number | null;
  chambreNumero: string | null;
  sejourId: number | null;
  agentNom: string | null;
  valideParNom: string | null;
  dateValidation: string | null;
  notes: string | null;
  createdAt: string;
}

export interface DepenseRequest {
  libelle: string;
  montant: number;
  famille: string;
  categorie?: string;
  dateDepense?: string;
  fournisseurId?: number | null;
  modeReglement?: string;
  referencePiece?: string;
  statut?: string;
  caisseId?: number | null;
  chambreId?: number | null;
  sejourId?: number | null;
  notes?: string;
}

export interface FournisseurResponse {
  id: number;
  nom: string;
  type: string;
  contact: string | null;
  telephone: string | null;
  email: string | null;
  adresse: string | null;
  notes: string | null;
  actif: boolean;
}

export interface FournisseurRequest {
  nom: string;
  type?: string;
  contact?: string;
  telephone?: string;
  email?: string;
  adresse?: string;
  notes?: string;
  actif?: boolean;
}

/** Les postes de charge, dans l'ordre où l'exploitant les rencontre au quotidien. */
export const FAMILLE_DEPENSE_OPTIONS = [
  { label: 'Restauration',      value: 'RESTAURATION' },
  { label: 'Provisions',        value: 'PROVISIONS' },
  { label: 'Maintenance',       value: 'MAINTENANCE' },
  { label: 'Entretien',         value: 'ENTRETIEN' },
  { label: 'Lingerie',          value: 'LINGERIE' },
  { label: 'Énergie & eau',     value: 'ENERGIE' },
  { label: 'Salaires',          value: 'SALAIRES' },
  { label: 'Charges locatives', value: 'CHARGES_LOCATIVES' },
  { label: 'Marketing',         value: 'MARKETING' },
  { label: 'Transport',         value: 'TRANSPORT' },
  { label: 'Administratif',     value: 'ADMINISTRATIF' },
  { label: 'Taxes & impôts',    value: 'TAXES' },
  { label: 'Divers',            value: 'DIVERS' }
];

export const FAMILLE_DEPENSE_LABELS: Record<string, string> =
  FAMILLE_DEPENSE_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

export const FAMILLE_DEPENSE_ICONS: Record<string, string> = {
  RESTAURATION:      'ti-tools-kitchen-2',
  PROVISIONS:        'ti-shopping-cart',
  MAINTENANCE:       'ti-tool',
  ENTRETIEN:         'ti-spray',
  LINGERIE:          'ti-wash',
  ENERGIE:           'ti-bolt',
  SALAIRES:          'ti-users',
  CHARGES_LOCATIVES: 'ti-building',
  MARKETING:         'ti-speakerphone',
  TRANSPORT:         'ti-car',
  ADMINISTRATIF:     'ti-file-text',
  TAXES:             'ti-receipt-tax',
  DIVERS:            'ti-dots'
};

export const STATUT_DEPENSE_LABELS: Record<string, string> = {
  EN_ATTENTE: 'En attente',
  VALIDEE:    'Validée',
  PAYEE:      'Réglée',
  REJETEE:    'Rejetée'
};

export const TYPE_FOURNISSEUR_OPTIONS = [
  { label: 'Alimentaire', value: 'ALIMENTAIRE' },
  { label: 'Boisson',     value: 'BOISSON' },
  { label: 'Entretien',   value: 'ENTRETIEN' },
  { label: 'Maintenance', value: 'MAINTENANCE' },
  { label: 'Lingerie',    value: 'LINGERIE' },
  { label: 'Mobilier',    value: 'MOBILIER' },
  { label: 'Énergie',     value: 'ENERGIE' },
  { label: 'Services',    value: 'SERVICES' },
  { label: 'Divers',      value: 'DIVERS' }
];
