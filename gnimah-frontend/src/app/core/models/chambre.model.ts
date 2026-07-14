export type EtatChambre = 'LIBRE' | 'OCCUPEE' | 'A_NETTOYER' | 'EN_MAINTENANCE' | 'HORS_SERVICE';
export type TypeChambre = 'STANDARD' | 'SUPERIEURE' | 'DELUXE' | 'SUITE' | 'FAMILIALE';

export interface ChambreRequest {
  numero: string;
  type: TypeChambre;
  capacite: number;
  tarifPassage: number;
  tarifNuitee: number;
  etat?: EtatChambre;
  etage?: number;
  description?: string;
  equipements?: string;
}

export interface ChambreResponse {
  id: number;
  numero: string;
  type: TypeChambre;
  capacite: number;
  tarifPassage: number;
  tarifNuitee: number;
  etat: EtatChambre;
  etage: number;
  description: string;
  equipements: string;
  actif: boolean;
  disponible: boolean;
}

export const ETAT_CHAMBRE_LABELS: Record<EtatChambre, string> = {
  LIBRE: 'Libre',
  OCCUPEE: 'Occupée',
  A_NETTOYER: 'À nettoyer',
  EN_MAINTENANCE: 'En maintenance',
  HORS_SERVICE: 'Hors service'
};

export const ETAT_CHAMBRE_COLORS: Record<EtatChambre, string> = {
  LIBRE: '#22c55e',
  OCCUPEE: '#ef4444',
  A_NETTOYER: '#f59e0b',
  EN_MAINTENANCE: '#8b5cf6',
  HORS_SERVICE: '#6b7280'
};

export const ETAT_CHAMBRE_BG: Record<EtatChambre, string> = {
  LIBRE: 'bg-green-100 text-green-800',
  OCCUPEE: 'bg-red-100 text-red-800',
  A_NETTOYER: 'bg-yellow-100 text-yellow-800',
  EN_MAINTENANCE: 'bg-purple-100 text-purple-800',
  HORS_SERVICE: 'bg-gray-100 text-gray-800'
};
