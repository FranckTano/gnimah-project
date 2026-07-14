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
  sejoursParType: { type: string; count: number }[];
  performanceAgents: { agentId: number; nbSejours: number; ca: number }[];
  occupationParChambre: any[];
}
