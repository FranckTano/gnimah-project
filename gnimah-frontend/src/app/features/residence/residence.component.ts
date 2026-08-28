import { Component, HostListener, OnInit } from '@angular/core';
import { PageHeaderService } from '../../core/services/page-header.service';
import { ChambreService } from '../../core/services/chambre.service';
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
export class ResidenceComponent implements OnInit {
  video!: MediaService['video'];
  photos: HotelPhoto[] = [];
  categories: (CategoriePhoto | 'Tout')[] = [];
  categorieActive: CategoriePhoto | 'Tout' = 'Tout';

  videoLancee = false;
  totalChambres: number | null = null;

  /** Visionneuse : index dans `photosFiltrees`, ou -1 si elle est fermée. */
  indexVisionneuse = -1;

  constructor(
    private media: MediaService,
    private chambreService: ChambreService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('La Résidence', 'Vidéo de présentation et album photo de l\'établissement');
    this.video = this.media.video;
    this.photos = this.media.photos;
    this.categories = ['Tout', ...this.media.categories];
    this.chambreService.findAll().subscribe({
      next: (c) => { this.totalChambres = c.length; },
      error: () => { /* la galerie reste consultable sans le compteur */ }
    });
  }

  get photosFiltrees(): HotelPhoto[] {
    return this.categorieActive === 'Tout'
      ? this.photos
      : this.photos.filter(p => p.categorie === this.categorieActive);
  }

  compte(c: CategoriePhoto | 'Tout'): number {
    return c === 'Tout' ? this.photos.length : this.photos.filter(p => p.categorie === c).length;
  }

  choisirCategorie(c: CategoriePhoto | 'Tout'): void {
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
