export interface CaisseResponse {
  id: number;
  nom: string;
  type: string;
  solde: number;
  responsableId: number | null;
  responsableNom: string | null;
  description: string | null;
  actif: boolean;
  sessionOuverteId: number | null;
  sessionCaissierNom: string | null;
}

export interface SessionCaisseResponse {
  id: number;
  caisseId: number;
  caisseNom: string;
  caissierId: number | null;
  caissierNom: string | null;
  dateOuverture: string;
  fondCaisse: number;
  dateFermeture: string | null;
  /** Activité de la vacation, tous modes de règlement. */
  totalEntrees: number;
  totalSorties: number;
  /** Part en espèces : c'est elle qui fait varier le tiroir. */
  entreesEspeces: number;
  sortiesEspeces: number;
  /** Fond de caisse + entrées en espèces − sorties en espèces. */
  montantTheorique: number;
  montantCompte: number | null;
  ecart: number;
  statut: 'OUVERTE' | 'FERMEE';
  fermeParNom: string | null;
  notes: string | null;
  nbMouvements: number;
}

export interface MouvementCaisseResponse {
  id: number;
  caisseId: number;
  caisseNom: string;
  sessionId: number | null;
  sens: 'ENTREE' | 'SORTIE';
  type: string;
  montant: number;
  mode: string;
  libelle: string;
  reference: string | null;
  origineType: string | null;
  origineId: number | null;
  soldeApres: number | null;
  agentNom: string | null;
  dateMouvement: string;
  notes: string | null;
}

export interface OuvertureCaisseRequest {
  caisseId: number;
  fondCaisse?: number;
  notes?: string;
}

export interface FermetureCaisseRequest {
  montantCompte: number;
  notes?: string;
}

export interface MouvementCaisseRequest {
  caisseId?: number | null;
  sens: 'ENTREE' | 'SORTIE';
  type: string;
  montant: number;
  mode?: string;
  libelle?: string;
  reference?: string;
  notes?: string;
}

export interface CaisseRequest {
  nom: string;
  type?: string;
  responsableId?: number | null;
  description?: string;
  actif?: boolean;
}

export const TYPE_MOUVEMENT_LABELS: Record<string, string> = {
  FOND_DE_CAISSE:      'Fond de caisse',
  ENCAISSEMENT_SEJOUR: 'Encaissement séjour',
  ACOMPTE_RESERVATION: 'Acompte réservation',
  VENTE_RESTAURATION:  'Vente restauration',
  RECETTE_EVENEMENT:   'Recette événement',
  AUTRE_RECETTE:       'Autre recette',
  REMBOURSEMENT:       'Remboursement',
  DEPENSE:             'Dépense',
  ACHAT_PROVISION:     'Achat de provisions',
  VERSEMENT_BANQUE:    'Versement bancaire',
  TRANSFERT:           'Transfert',
  AJUSTEMENT:          'Ajustement'
};

/** Types proposés à la saisie manuelle, par sens — les autres naissent d'une opération métier. */
export const TYPES_ENTREE_MANUELS = [
  { label: 'Autre recette',      value: 'AUTRE_RECETTE' },
  { label: 'Recette événement',  value: 'RECETTE_EVENEMENT' },
  { label: 'Fond de caisse',     value: 'FOND_DE_CAISSE' },
  { label: 'Transfert entrant',  value: 'TRANSFERT' },
  { label: 'Ajustement',         value: 'AJUSTEMENT' }
];

export const TYPES_SORTIE_MANUELS = [
  { label: 'Dépense',            value: 'DEPENSE' },
  { label: 'Remboursement',      value: 'REMBOURSEMENT' },
  { label: 'Achat de provisions', value: 'ACHAT_PROVISION' },
  { label: 'Transfert sortant',  value: 'TRANSFERT' },
  { label: 'Ajustement',         value: 'AJUSTEMENT' }
];

export const TYPE_CAISSE_OPTIONS = [
  { label: 'Réception',    value: 'RECEPTION' },
  { label: 'Restauration', value: 'RESTAURATION' },
  { label: 'Événementiel', value: 'EVENEMENTIEL' },
  { label: 'Coffre',       value: 'COFFRE' },
  { label: 'Autre',        value: 'AUTRE' }
];
