import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { PageHeaderService } from '../../core/services/page-header.service';
import { ChambreService } from '../../core/services/chambre.service';
import { AuthService } from '../../core/services/auth.service';
import { MediaService, HotelPhoto, CategoriePhoto } from '../../core/services/media.service';

/**
 * « La Résidence » — la vitrine interne de l'hôtel : la vidéo de présentation et
 * l'album photo. Sert de repère visuel à l'équipe (à quoi ressemble telle chambre)
 * et donne un visage au logiciel de gestion.
 */
@Component({
  selector: 'app-residence',
  standalone: false,
  templateUrl: './residence.component.html',
  styleUrl: './residence.component.scss'
})
export class ResidenceComponent implements OnInit, OnDestroy {
  video!: MediaService['video'];
  photos: HotelPhoto[] = [];
  categories: (CategoriePhoto | 'Tout' | string)[] = [];
  categorieActive: CategoriePhoto | 'Tout' | string = 'Tout';

  videoLancee = false;
  totalChambres: number | null = null;
  peutAdministrer = false;

  /** Visionneuse : index dans `photosFiltrees`, ou -1 si elle est fermée. */
  indexVisionneuse = -1;

  private sub = new Subscription();

  constructor(
    private media: MediaService,
    private chambreService: ChambreService,
    private authService: AuthService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('La Résidence', 'Vidéo de présentation et album photo de l\'établissement');
    this.video = this.media.video;
    this.peutAdministrer = this.authService.hasDirecteurAccess();

    // La galerie est administrable : on suit ses mises à jour plutôt que d'en
    // prendre une copie figée au chargement.
    this.sub.add(this.media.photos$.subscribe(photos => {
      this.photos = photos;
      this.majCategories();
      // Une catégorie devenue vide (dernier visuel supprimé) laisserait la page
      // blanche sans explication : on revient sur « Tout ».
      if (this.categorieActive !== 'Tout' && !this.categories.includes(this.categorieActive)) {
        this.categorieActive = 'Tout';
      }
      if (this.indexVisionneuse >= this.photosFiltrees.length) this.indexVisionneuse = -1;
    }));

    this.chambreService.findAll().subscribe({
      next: (c) => { this.totalChambres = c.length; },
      error: () => { /* la galerie reste consultable sans le compteur */ }
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  /**
   * Catégories réellement présentes dans la galerie. L'administrateur peut en
   * saisir de nouvelles : la liste se déduit du contenu au lieu d'être figée.
   */
  private majCategories(): void {
    const presentes = [...new Set(this.photos.map(p => p.categorie))];
    const connues = this.media.categories.filter(c => presentes.includes(c));
    const inedites = presentes.filter(c => !this.media.categories.includes(c as CategoriePhoto));
    this.categories = ['Tout', ...connues, ...inedites];
  }

  get photosFiltrees(): HotelPhoto[] {
    return this.categorieActive === 'Tout'
      ? this.photos
      : this.photos.filter(p => p.categorie === this.categorieActive);
  }

  compte(c: string): number {
    return c === 'Tout' ? this.photos.length : this.photos.filter(p => p.categorie === c).length;
  }

  choisirCategorie(c: string): void {
    this.categorieActive = c;
    this.indexVisionneuse = -1;
  }

  lancerVideo(): void {
    this.videoLancee = true;
  }

  // ---- Visionneuse ---------------------------------------------------------

  ouvrir(i: number): void {
    this.indexVisionneuse = i;
  }

  fermer(): void {
    this.indexVisionneuse = -1;
  }

  get photoVisionneuse(): HotelPhoto | null {
    const liste = this.photosFiltrees;
    return this.indexVisionneuse >= 0 && this.indexVisionneuse < liste.length
      ? liste[this.indexVisionneuse]
      : null;
  }

  naviguer(pas: number, evt?: Event): void {
    evt?.stopPropagation();
    const n = this.photosFiltrees.length;
    if (!n) return;
    this.indexVisionneuse = (this.indexVisionneuse + pas + n) % n;
  }

  @HostListener('document:keydown', ['$event'])
  auClavier(e: KeyboardEvent): void {
    if (this.indexVisionneuse < 0) return;
    if (e.key === 'Escape') this.fermer();
    else if (e.key === 'ArrowRight') this.naviguer(1);
    else if (e.key === 'ArrowLeft') this.naviguer(-1);
  }
}
