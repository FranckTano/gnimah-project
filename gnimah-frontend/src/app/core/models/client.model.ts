export interface ClientRequest {
  civilite: string;
  nom: string;
  prenom?: string;
  telephone: string;
  email?: string;
  typePiece: string;
  numeroPiece: string;
  nationalite?: string;
  adresse?: string;
}

export interface ClientResponse {
  id: number;
  civilite: string;
  nom: string;
  prenom: string;
  nomComplet: string;
  telephone: string;
  email: string;
  typePiece: string;
  numeroPiece: string;
  nationalite: string;
  adresse: string;
  nbSejours: number;
  clientFidele: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
