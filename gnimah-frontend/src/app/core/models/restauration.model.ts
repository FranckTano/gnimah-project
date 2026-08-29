export interface ArticleRestaurationResponse {
  id: number;
  code: string | null;
  nom: string;
  categorie: string;
  description: string | null;
  prixVente: number;
  coutRevient: number;
  marge: number;
  disponible: boolean;
  photo: string | null;
}

export interface ArticleRestaurationRequest {
  code?: string;
  nom: string;
  categorie: string;
  description?: string;
  prixVente: number;
  coutRevient?: number;
  disponible?: boolean;
  photo?: string;
}

export interface LigneCommandeResponse {
  id: number;
  articleId: number | null;
  designation: string;
  quantite: number;
  prixUnitaire: number;
  montant: number;
}

export interface LigneCommandeRequest {
  articleId?: number | null;
  designation?: string;
  quantite: number;
  prixUnitaire?: number;
}

export type StatutCommande = 'OUVERTE' | 'ENVOYEE_CUISINE' | 'SERVIE' | 'PAYEE' | 'ANNULEE';

export interface CommandeResponse {
  id: number;
  numero: string;
  type: string;
  tableNumero: string | null;
  sejourId: number | null;
  sejourChambre: string | null;
  clientId: number | null;
  clientNom: string | null;
  evenementId: number | null;
  evenementTitre: string | null;
  montantBrut: number;
  remiseType: string | null;
  remiseValeur: number;
  remiseMontant: number;
  remiseMotif: string | null;
  montantNet: number;
  montantPaye: number;
  statut: StatutCommande;
  modePaiement: string | null;
  imputeSejour: boolean;
  agentNom: string | null;
  dateCommande: string;
  datePaiement: string | null;
  notes: string | null;
  lignes: LigneCommandeResponse[];
}

export interface CommandeRequest {
  type?: string;
  tableNumero?: string;
  sejourId?: number | null;
  clientId?: number | null;
  evenementId?: number | null;
  imputeSejour?: boolean;
  remiseType?: string | null;
  remiseValeur?: number | null;
  remiseMotif?: string | null;
  notes?: string;
  lignes: LigneCommandeRequest[];
}

export interface PaiementCommandeRequest {
  mode: string;
  referenceTransaction?: string;
  imputeSejour?: boolean;
  notes?: string;
}

export const CATEGORIE_RESTO_OPTIONS = [
  { label: 'Petit-déjeuner',  value: 'PETIT_DEJEUNER' },
  { label: 'Entrée',          value: 'ENTREE' },
  { label: 'Plat',            value: 'PLAT' },
  { label: 'Accompagnement',  value: 'ACCOMPAGNEMENT' },
  { label: 'Dessert',         value: 'DESSERT' },
  { label: 'Boisson froide',  value: 'BOISSON_FROIDE' },
  { label: 'Boisson chaude',  value: 'BOISSON_CHAUDE' },
  { label: 'Alcool',          value: 'ALCOOL' },
  { label: 'Snack',           value: 'SNACK' }
];

export const CATEGORIE_RESTO_LABELS: Record<string, string> =
  CATEGORIE_RESTO_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

export const CATEGORIE_RESTO_ICONS: Record<string, string> = {
  PETIT_DEJEUNER: 'ti-coffee',
  ENTREE:         'ti-salad',
  PLAT:           'ti-tools-kitchen-2',
  ACCOMPAGNEMENT: 'ti-bowl',
  DESSERT:        'ti-cake',
  BOISSON_FROIDE: 'ti-glass',
  BOISSON_CHAUDE: 'ti-cup',
  ALCOOL:         'ti-beer',
  SNACK:          'ti-cookie'
};

export const TYPE_COMMANDE_OPTIONS = [
  { label: 'Sur place',    value: 'SUR_PLACE' },
  { label: 'Room service', value: 'ROOM_SERVICE' },
  { label: 'À emporter',   value: 'A_EMPORTER' },
  { label: 'Événement',    value: 'EVENEMENT' }
];

export const TYPE_COMMANDE_LABELS: Record<string, string> =
  TYPE_COMMANDE_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

export const STATUT_COMMANDE_LABELS: Record<string, string> = {
  OUVERTE:         'Ouverte',
  ENVOYEE_CUISINE: 'En cuisine',
  SERVIE:          'Servie',
  PAYEE:           'Réglée',
  ANNULEE:         'Annulée'
};
