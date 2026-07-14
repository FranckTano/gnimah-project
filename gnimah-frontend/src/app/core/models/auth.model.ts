export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  userId: number;
  username: string;
  nom: string;
  prenom: string;
  role: 'ADMIN' | 'DIRECTEUR' | 'AGENT';
  email: string;
}

export interface CurrentUser {
  userId: number;
  username: string;
  nom: string;
  prenom: string;
  role: 'ADMIN' | 'DIRECTEUR' | 'AGENT';
  email: string;
  nomComplet: string;
}
