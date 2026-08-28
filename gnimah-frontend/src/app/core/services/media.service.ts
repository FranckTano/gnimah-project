import { Injectable } from '@angular/core';
import { TypeChambre } from '../models/chambre.model';
import { environment } from '../../../environments/environment';

export type CategoriePhoto = 'Extérieur' | 'Chambres' | 'Restauration' | 'Espaces communs' | 'Équipements';

export interface HotelPhoto {
  /** Slug stable — sert de clé de tri et d'identifiant dans la visionneuse. */
  id: string;
  /** Image pleine résolution (1264px de large) pour héros et visionneuse. */
  src: string;
  /** Vignette (620px) pour les grilles et les cartes de chambre. */
  thumb: string;
  titre: string;
  categorie: CategoriePhoto;
}

/**
 * Catalogue des photos réelles de la Résidence Hôtel GNIMAH.
 *
 * Un seul endroit décrit les visuels, pour que la galerie, l'écran de connexion,
 * le tableau de bord et les fiches chambres partagent la même source de vérité.
 *
 * Les URL se construisent à partir de `environment.media` : renseigné, les
 * fichiers viennent du CDN ; vide, ils viennent de `public/media/` embarqué dans
 * l'application. Le repli local garantit que l'app reste visuellement complète
 * si le CDN est injoignable, et évite de consommer du quota en développement.
 */
@Injectable({ providedIn: 'root' })
export class MediaService {
  /** Sans barre oblique finale, pour que la concaténation reste prévisible. */
  private readonly baseImages = (environment.media?.photos || '').replace(/\/+$/, '');
  private readonly baseVideo = (environment.media?.video || '').replace(/\/+$/, '');

  readonly video = {
    src: this.urlVideo('gnimah-presentation.mp4'),
    poster: this.urlImage('gnimah-presentation-poster.jpg', 'video'),
    titre: 'Visite guidée de la résidence',
    duree: '1 min 03'
  };

  /** `dossier` distingue l'affiche vidéo des photos de la galerie. */
  private urlImage(fichier: string, dossier: 'photos' | 'video' = 'photos'): string {
    return this.baseImages ? `${this.baseImages}/${dossier}/${fichier}` : `/media/${dossier}/${fichier}`;
  }

  private urlVideo(fichier: string): string {
    return this.baseVideo ? `${this.baseVideo}/video/${fichier}` : `/media/video/${fichier}`;
  }

  readonly photos: HotelPhoto[] = [
    this.photo('facade-patio', 'Patio & allée centrale', 'Extérieur'),
    this.photo('suite-lit-king', 'Suite — lit king size', 'Chambres'),
    this.photo('chambre-standard', 'Chambre standard climatisée', 'Chambres'),
    this.photo('chambre-confort', 'Chambre confort', 'Chambres'),
    this.photo('chambre-bureau', 'Chambre avec coin bureau', 'Chambres'),
    this.photo('chambre-fresque-ciel', 'Chambre — fresque ciel', 'Chambres'),
    this.photo('chambre-salon-tv', 'Coin salon & télévision', 'Chambres'),
    this.photo('chambre-tv-fresque', 'Chambre — écran plat', 'Chambres'),
    this.photo('chambre-coin-detente', 'Coin détente & minibar', 'Chambres'),
    this.photo('restaurant-salle', 'Salle de restauration', 'Restauration'),
    this.photo('salon-lounge', 'Salon lounge', 'Espaces communs'),
    this.photo('couloir-etage', 'Couloir des chambres', 'Espaces communs'),
    this.photo('salle-de-bain', 'Salle de bain privative', 'Équipements'),
    this.photo('equipement-chauffe-eau', 'Eau chaude en continu', 'Équipements')
  ];

  readonly categories: CategoriePhoto[] = [
    'Extérieur', 'Chambres', 'Espaces communs', 'Restauration', 'Équipements'
  ];

  /** Visuel signature de la résidence — bandeaux d'en-tête et affiche vidéo. */
  get heros(): HotelPhoto {
    return this.byId('facade-patio');
  }

  /** Rotation lente de l'écran de connexion : les plus flatteuses d'abord. */
  get diaporamaConnexion(): HotelPhoto[] {
    return ['facade-patio', 'suite-lit-king', 'salon-lounge', 'restaurant-salle', 'chambre-standard']
      .map(id => this.byId(id));
  }

  /** Bandeaux internes (tableau de bord, KPI) — cadrages larges et lumineux. */
  get bandeaux(): HotelPhoto[] {
    return ['facade-patio', 'salon-lounge', 'restaurant-salle', 'couloir-etage'].map(id => this.byId(id));
  }

  byId(id: string): HotelPhoto {
    return this.photos.find(p => p.id === id) ?? this.photos[0];
  }

  parCategorie(categorie: CategoriePhoto): HotelPhoto[] {
    return this.photos.filter(p => p.categorie === categorie);
  }

  /**
   * Visuel par défaut d'une chambre. Le numéro sert de graine pour que la même
   * chambre garde toujours la même photo d'une session à l'autre.
   */
  photoChambre(type: TypeChambre | string | null | undefined, numero?: string | null): HotelPhoto {
    const parType: Record<string, string[]> = {
      SUITE: ['suite-lit-king', 'chambre-coin-detente'],
      DELUXE: ['chambre-confort', 'chambre-bureau'],
      SUPERIEURE: ['chambre-standard', 'chambre-salon-tv'],
      FAMILIALE: ['chambre-fresque-ciel', 'chambre-tv-fresque'],
      STANDARD: ['chambre-standard', 'chambre-confort', 'chambre-fresque-ciel']
    };
    const pool = parType[(type || 'STANDARD').toString().toUpperCase()] ?? parType['STANDARD'];
    const graine = (numero || '').split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return this.byId(pool[graine % pool.length]);
  }

  private photo(id: string, titre: string, categorie: CategoriePhoto): HotelPhoto {
    return {
      id, titre, categorie,
      src: this.urlImage(`${id}.jpg`),
      thumb: this.urlImage(`${id}-sm.jpg`)
    };
  }
}
