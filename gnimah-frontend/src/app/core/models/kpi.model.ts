export interface KpiResponse {
  chiffreAffaires: number;
  encaissements: number;
  resteAPayer: number;
  depensesTotales: number;
  marge: number;
  panierMoyen: number;
  tauxOccupation: number;
  adr: number;
  revPar: number;
  tRevPar: number;
  alos: number;
  totalChambres: number;
  chambresLibres: number;
  chambresOccupees: number;
  chambresANettoyer: number;
  chambresEnMaintenance: number;
  totalSejours: number;
  nouveauxClients: number;
  clientsFideles: number;
  reservationsTotal: number;
  reservationsAnnulees: number;
  reservationsNoShow: number;
  tauxConversion: number;
  caParJour: { date: string; ca: number }[];
  reservationsParJour: { date: string; count: number }[];
  sejoursParType: { type: string; count: number }[];
  performanceAgents: { agentId: number; nbSejours: number; ca: number }[];
  occupationParChambre: any[];
  /** Renvoyé par le backend depuis `buildRepartitionModePaiement`. */
  repartitionModePaiement: { mode: string; count: number; montant: number }[];
}

/** Libellés francophones des modes de paiement renvoyés en enum par le backend. */
export const MODE_PAIEMENT_LABELS: Record<string, string> = {
  ESPECES: 'Espèces',
  MOBILE_MONEY: 'Mobile Money',
  CARTE_BANCAIRE: 'Carte bancaire',
  CARTE: 'Carte bancaire',
  VIREMENT: 'Virement',
  CHEQUE: 'Chèque',
  AUTRE: 'Autre'
};
