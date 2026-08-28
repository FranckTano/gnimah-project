export type CategoriePhoto = 'Extérieur' | 'Chambres' | 'Espaces communs' | 'Restauration' | 'Équipements';

/** Catégories proposées à l'administrateur ; la saisie libre reste possible. */
export const CATEGORIES_PHOTO: CategoriePhoto[] = [
  'Extérieur', 'Chambres', 'Espaces communs', 'Restauration', 'Équipements'
];

/** Un visuel de la galerie, tel que renvoyé par le backend. */
export interface MediaResidenceResponse {
  id: number;
  titre: string;
  categorie: string;
  url: string;
  urlVignette: string;
  publicId: string | null;
  position: number;
  actif: boolean;
  misEnAvant: boolean;
  /** Vrai si le fichier est hébergé par nous : la suppression l'effacera aussi. */
  heberge: boolean;
  createdAt: string | null;
}

export interface MediaResidenceRequest {
  titre: string;
  categorie: string;
  url?: string;
  urlVignette?: string;
  position?: number;
  actif?: boolean;
  misEnAvant?: boolean;
}
