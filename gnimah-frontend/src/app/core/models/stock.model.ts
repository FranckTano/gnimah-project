export interface ArticleStockResponse {
  id: number;
  code: string | null;
  designation: string;
  categorie: string;
  unite: string;
  quantite: number;
  seuilAlerte: number;
  prixUnitaire: number;
  valeurStock: number;
  fournisseurId: number | null;
  fournisseurNom: string | null;
  emplacement: string | null;
  actif: boolean;
  enAlerte: boolean;
}

export interface ArticleStockRequest {
  code?: string;
  designation: string;
  categorie: string;
  unite?: string;
  quantite?: number;
  seuilAlerte?: number;
  prixUnitaire?: number;
  fournisseurId?: number | null;
  emplacement?: string;
  actif?: boolean;
}

export interface MouvementStockResponse {
  id: number;
  articleId: number;
  articleDesignation: string;
  unite: string;
  sens: 'ENTREE' | 'SORTIE';
  type: string;
  quantite: number;
  quantiteApres: number | null;
  prixUnitaire: number;
  montant: number;
  fournisseurNom: string | null;
  depenseId: number | null;
  chambreNumero: string | null;
  agentNom: string | null;
  dateMouvement: string;
  motif: string | null;
}

export interface MouvementStockRequest {
  articleId: number;
  type: string;
  quantite: number;
  prixUnitaire?: number;
  fournisseurId?: number | null;
  chambreId?: number | null;
  genererDepense?: boolean;
  caisseId?: number | null;
  modeReglement?: string;
  motif?: string;
}

export const CATEGORIE_STOCK_OPTIONS = [
  { label: 'Provisions alimentaires', value: 'PROVISION_ALIMENTAIRE' },
  { label: 'Boissons',                value: 'BOISSON' },
  { label: "Produits d'entretien",    value: 'PRODUIT_ENTRETIEN' },
  { label: 'Linge',                   value: 'LINGE' },
  { label: 'Matériel de chambre',     value: 'MATERIEL_CHAMBRE' },
  { label: 'Matériel de maintenance', value: 'MATERIEL_MAINTENANCE' },
  { label: 'Fournitures de bureau',   value: 'FOURNITURE_BUREAU' },
  { label: 'Autre',                   value: 'AUTRE' }
];

export const CATEGORIE_STOCK_LABELS: Record<string, string> =
  CATEGORIE_STOCK_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

export const CATEGORIE_STOCK_ICONS: Record<string, string> = {
  PROVISION_ALIMENTAIRE: 'ti-basket',
  BOISSON:               'ti-bottle',
  PRODUIT_ENTRETIEN:     'ti-spray',
  LINGE:                 'ti-wash',
  MATERIEL_CHAMBRE:      'ti-bed',
  MATERIEL_MAINTENANCE:  'ti-tool',
  FOURNITURE_BUREAU:     'ti-paperclip',
  AUTRE:                 'ti-package'
};

export const UNITE_OPTIONS = [
  { label: 'Pièce',      value: 'PIECE' },
  { label: 'Kg',         value: 'KG' },
  { label: 'Gramme',     value: 'GRAMME' },
  { label: 'Litre',      value: 'LITRE' },
  { label: 'Centilitre', value: 'CENTILITRE' },
  { label: 'Carton',     value: 'CARTON' },
  { label: 'Sac',        value: 'SAC' },
  { label: 'Paquet',     value: 'PAQUET' },
  { label: 'Bouteille',  value: 'BOUTEILLE' },
  { label: 'Rouleau',    value: 'ROULEAU' },
  { label: 'Mètre',      value: 'METRE' }
];

export const UNITE_LABELS: Record<string, string> =
  UNITE_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

export const TYPE_MOUVEMENT_STOCK_OPTIONS = [
  { label: 'Achat / réapprovisionnement', value: 'ACHAT' },
  { label: 'Consommation restauration',   value: 'CONSOMMATION_RESTAURATION' },
  { label: 'Consommation chambre',        value: 'CONSOMMATION_CHAMBRE' },
  { label: 'Perte',                       value: 'PERTE' },
  { label: 'Casse',                       value: 'CASSE' },
  { label: 'Correction d\'inventaire',    value: 'INVENTAIRE' },
  { label: 'Retour fournisseur',          value: 'RETOUR_FOURNISSEUR' },
  { label: 'Transfert',                   value: 'TRANSFERT' }
];

export const TYPE_MOUVEMENT_STOCK_LABELS: Record<string, string> =
  TYPE_MOUVEMENT_STOCK_OPTIONS.reduce((acc, o) => ({ ...acc, [o.value]: o.label }), {});

/** Les types qui font entrer de la marchandise ; les autres en font sortir. */
export const TYPES_ENTREE_STOCK = ['ACHAT', 'INVENTAIRE'];
