import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { TypeChambre } from '../models/chambre.model';
import { CategoriePhoto, MediaResidenceResponse } from '../models/media.model';
import { MediaApiService } from './media-api.service';
import { environment } from '../../../environments/environment';

/** Ré-export de commodité : les composants n'importent alors qu'un seul module. */
export type { CategoriePhoto };

export interface HotelPhoto {
  /** Identifiant stable. Numérique pour un visuel administrable, slug pour le catalogue intégré. */
  id: string;
  /** Image pleine résolution, pour héros et visionneuse. */
  src: string;
  /** Vignette, pour les grilles et les cartes de chambre. */
  thumb: string;
  titre: string;
  categorie: CategoriePhoto | string;
}

/**
 * Visuels de la Résidence Hôtel GNIMAH.
 *
 * La galerie est administrable en base (voir {@link MediaApiService}) : ce
 * service en tient une copie à jour et la publie via {@link photos$}.
 *
 * Le catalogue ci-dessous reste le point de départ. Il sert :
 *  — à l'écran de connexion, qui n'est pas authentifié et ne peut donc pas
 *    interroger l'API ;
 *  — au tout premier rendu, avant que la requête n'ait abouti ;
 *  — de secours si l'API est injoignable.
 * L'application n'est ainsi jamais sans visuel, quel que soit l'état du réseau.
 */
@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly api = inject(MediaApiService);

  /** Sans barre oblique finale, pour que la concaténation reste prévisible. */
  private readonly baseImages = (environment.media?.photos || '').replace(/\/+$/, '');
  private readonly baseVideo = (environment.media?.video || '').replace(/\/+$/, '');

  readonly video = {
    src: this.urlVideo('gnimah-presentation.mp4'),
    poster: this.urlImage('gnimah-presentation-poster.jpg', 'video'),
    titre: 'Visite guidée de la résidence',
    duree: '1 min 03'
  };

  readonly categories: CategoriePhoto[] = [
    'Extérieur', 'Chambres', 'Espaces communs', 'Restauration', 'Équipements'
  ];

  /** Catalogue livré avec l'application — jamais vide, jamais dépendant du réseau. */
  private readonly catalogueIntegre: HotelPhoto[] = [
    this.photoIntegree('facade-patio', 'Patio & allée centrale', 'Extérieur'),
    this.photoIntegree('suite-lit-king', 'Suite — lit king size', 'Chambres'),
    this.photoIntegree('chambre-standard', 'Chambre standard climatisée', 'Chambres'),
    this.photoIntegree('chambre-confort', 'Chambre confort', 'Chambres'),
    this.photoIntegree('chambre-bureau', 'Chambre avec coin bureau', 'Chambres'),
    this.photoIntegree('chambre-fresque-ciel', 'Chambre — fresque ciel', 'Chambres'),
    this.photoIntegree('chambre-salon-tv', 'Coin salon & télévision', 'Chambres'),
    this.photoIntegree('chambre-tv-fresque', 'Chambre — écran plat', 'Chambres'),
    this.photoIntegree('chambre-coin-detente', 'Coin détente & minibar', 'Chambres'),
    this.photoIntegree('restaurant-salle', 'Salle de restauration', 'Restauration'),
    this.photoIntegree('salon-lounge', 'Salon lounge', 'Espaces communs'),
    this.photoIntegree('couloir-etage', 'Couloir des chambres', 'Espaces communs'),
    this.photoIntegree('salle-de-bain', 'Salle de bain privative', 'Équipements'),
    this.photoIntegree('equipement-chauffe-eau', 'Eau chaude en continu', 'Équipements')
  ];

  private readonly _photos = new BehaviorSubject<HotelPhoto[]>(this.catalogueIntegre);
  /** Galerie courante. Émet à nouveau après chaque rechargement depuis l'API. */
  readonly photos$: Observable<HotelPhoto[]> = this._photos.asObservable();

  /** Visuel mis en avant par l'administrateur, à défaut le premier de la galerie. */
  private _heros: HotelPhoto = this.catalogueIntegre[0];

  /** Lecture synchrone, pour les composants qui n'ont pas besoin de réagir. */
  get photos(): HotelPhoto[] {
    return this._photos.value;
  }

  get heros(): HotelPhoto {
    return this._heros;
  }

  /**
   * Recharge la galerie depuis l'API. Appelé par la mise en page, donc seulement
   * en session authentifiée. Un échec est silencieux : le catalogue intégré
   * reste affiché plutôt qu'une page sans image.
   */
  charger(): void {
    this.api.findVisibles().subscribe({
      next: (liste) => this.appliquer(liste),
      error: () => { /* on garde le catalogue intégré */ }
    });
  }

  /** Appelé par l'administration après une modification, pour propager aussitôt. */
  appliquer(liste: MediaResidenceResponse[]): void {
    const visibles = (liste || []).filter(m => m.actif);
    if (!visibles.length) {
      // Une galerie vidée par mégarde ne doit pas dépouiller toute l'application.
      this._photos.next(this.catalogueIntegre);
      this._heros = this.catalogueIntegre[0];
      return;
    }
    const photos: HotelPhoto[] = visibles.map(m => ({
      id: String(m.id),
      src: m.url,
      thumb: m.urlVignette || m.url,
      titre: m.titre,
      categorie: m.categorie
    }));
    this._photos.next(photos);

    const enAvant = visibles.find(m => m.misEnAvant);
    this._heros = enAvant
      ? photos[visibles.indexOf(enAvant)]
      : photos[0];
  }

  /** Bandeaux internes : le visuel mis en avant, puis les suivants. */
  get bandeaux(): HotelPhoto[] {
    const liste = this.photos;
    const heros = this._heros;
    const autres = liste.filter(p => p.id !== heros.id);
    return [heros, ...autres].slice(0, 4);
  }

  /**
   * Diaporama de l'écran de connexion. Toujours le catalogue intégré : cette page
   * s'affiche avant toute authentification et ne peut donc pas interroger l'API.
   */
  get diaporamaConnexion(): HotelPhoto[] {
    return ['facade-patio', 'suite-lit-king', 'salon-lounge', 'restaurant-salle', 'chambre-standard']
      .map(id => this.catalogueIntegre.find(p => p.id === id))
      .filter((p): p is HotelPhoto => !!p);
  }

  parCategorie(categorie: string): HotelPhoto[] {
    return this.photos.filter(p => p.categorie === categorie);
  }

  /**
   * Visuel par défaut d'une chambre. Le numéro sert de graine pour que la même
   * chambre garde toujours la même photo d'une session à l'autre.
   *
   * On puise dans la catégorie « Chambres » de la galerie courante : les visuels
   * ajoutés par l'administrateur entrent donc naturellement dans la rotation.
   */
  photoChambre(type: TypeChambre | string | null | undefined, numero?: string | null): HotelPhoto {
    const chambres = this.photos.filter(p => p.categorie === 'Chambres');
    const pool = chambres.length ? chambres : this.photos;
    if (!pool.length) return this.catalogueIntegre[0];

    // Le type entre dans la graine : deux chambres de types différents ne
    // tombent pas systématiquement sur le même visuel.
    const graine = `${type || ''}${numero || ''}`
      .split('')
      .reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return pool[graine % pool.length];
  }

  // ---- Construction des URL du catalogue intégré ----------------------------

  /** `dossier` distingue l'affiche vidéo des photos de la galerie. */
  private urlImage(fichier: string, dossier: 'photos' | 'video' = 'photos'): string {
    return this.baseImages ? `${this.baseImages}/${dossier}/${fichier}` : `/media/${dossier}/${fichier}`;
  }

  private urlVideo(fichier: string): string {
    return this.baseVideo ? `${this.baseVideo}/video/${fichier}` : `/media/video/${fichier}`;
  }

  private photoIntegree(id: string, titre: string, categorie: CategoriePhoto): HotelPhoto {
    return {
      id, titre, categorie,
      src: this.urlImage(`${id}.jpg`),
      thumb: this.urlImage(`${id}-sm.jpg`)
    };
  }
}
