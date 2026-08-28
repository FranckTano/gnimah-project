import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PageHeaderService } from '../../../core/services/page-header.service';
import { MediaApiService } from '../../../core/services/media-api.service';
import { MediaService } from '../../../core/services/media.service';
import { CATEGORIES_PHOTO, MediaResidenceResponse } from '../../../core/models/media.model';

/**
 * Administration de la galerie « La Résidence ».
 *
 * Permet d'ajouter, renommer, réordonner, masquer ou supprimer un visuel sans
 * toucher au code : le catalogue vivait auparavant en dur dans le frontend et
 * chaque photo demandait un redéploiement.
 */
@Component({
  selector: 'app-medias',
  standalone: false,
  templateUrl: './medias.component.html',
  styleUrl: './medias.component.scss'
})
export class MediasComponent implements OnInit {
  medias: MediaResidenceResponse[] = [];
  loading = false;
  categories = CATEGORIES_PHOTO;

  /** Faux si le serveur n'a pas d'identifiants Cloudinary : on masque l'envoi. */
  hebergementDisponible = true;

  // ---- Envoi de fichier ----------------------------------------------------
  fichier: File | null = null;
  apercu: string | null = null;
  titreEnvoi = '';
  categorieEnvoi: string = 'Chambres';
  envoiEnCours = false;

  // ---- Édition -------------------------------------------------------------
  dialogVisible = false;
  editForm!: FormGroup;
  editing: MediaResidenceResponse | null = null;
  enregistrement = false;

  // ---- Ajout par adresse ---------------------------------------------------
  dialogUrlVisible = false;
  urlForm!: FormGroup;

  constructor(
    private api: MediaApiService,
    private media: MediaService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Photos de la résidence', 'Ajouter, ordonner et publier les visuels de l\'établissement');
    this.editForm = this.fb.group({
      titre: ['', [Validators.required, Validators.maxLength(150)]],
      categorie: ['Chambres', Validators.required],
      actif: [true],
      misEnAvant: [false]
    });
    this.urlForm = this.fb.group({
      titre: ['', [Validators.required, Validators.maxLength(150)]],
      categorie: ['Chambres', Validators.required],
      url: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
      urlVignette: ['']
    });

    this.api.capacites().subscribe({
      next: (c) => { this.hebergementDisponible = c.hebergementDisponible; },
      error: () => { this.hebergementDisponible = false; }
    });
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.api.findAll().subscribe({
      next: (liste) => {
        this.medias = liste;
        this.loading = false;
        // Propage aussitôt : la galerie et les bandeaux reflètent la modification
        // sans attendre un rechargement de page.
        this.media.appliquer(liste);
      },
      error: () => { this.loading = false; }
    });
  }

  get nbActifs(): number {
    return this.medias.filter(m => m.actif).length;
  }

  // ==========================================================================
  //  Envoi de fichier
  // ==========================================================================

  onFichier(event: Event): void {
    const input = event.target as HTMLInputElement;
    const f = input.files?.[0];
    if (!f) return;

    if (!f.type.startsWith('image/')) {
      this.messageService.add({ severity: 'warn', summary: 'Format non accepté', detail: 'Choisissez une image (JPG, PNG ou WebP).' });
      input.value = '';
      return;
    }
    // Le serveur refuse au-delà de 10 Mo : autant le dire avant l'envoi plutôt
    // que de faire patienter sur un transfert voué à l'échec.
    if (f.size > 10 * 1024 * 1024) {
      this.messageService.add({ severity: 'warn', summary: 'Image trop lourde', detail: 'La photo ne doit pas dépasser 10 Mo.' });
      input.value = '';
      return;
    }

    this.fichier = f;
    if (!this.titreEnvoi) {
      this.titreEnvoi = f.name.replace(/\.[^.]+$/, '').replace(/[-_]+/g, ' ').trim();
    }
    const lecteur = new FileReader();
    lecteur.onload = () => { this.apercu = lecteur.result as string; };
    lecteur.readAsDataURL(f);
  }

  annulerEnvoi(): void {
    this.fichier = null;
    this.apercu = null;
    this.titreEnvoi = '';
  }

  envoyer(): void {
    if (!this.fichier || this.envoiEnCours) return;
    this.envoiEnCours = true;
    this.api.televerser(this.fichier, this.titreEnvoi.trim(), this.categorieEnvoi).subscribe({
      next: () => {
        this.envoiEnCours = false;
        this.annulerEnvoi();
        this.messageService.add({ severity: 'success', summary: 'Photo ajoutée', detail: 'Le visuel est publié dans la galerie.' });
        this.charger();
      },
      error: (err) => {
        this.envoiEnCours = false;
        this.messageService.add({
          severity: 'error', summary: 'Envoi impossible',
          detail: err?.error?.message || "L'envoi de l'image a échoué.", life: 8000
        });
      }
    });
  }

  // ==========================================================================
  //  Édition
  // ==========================================================================

  ouvrirEdition(m: MediaResidenceResponse): void {
    this.editing = m;
    this.editForm.reset({
      titre: m.titre, categorie: m.categorie, actif: m.actif, misEnAvant: m.misEnAvant
    });
    this.dialogVisible = true;
  }

  enregistrer(): void {
    if (this.editForm.invalid || !this.editing) return;
    this.enregistrement = true;
    this.api.update(this.editing.id, this.editForm.value).subscribe({
      next: () => {
        this.enregistrement = false;
        this.dialogVisible = false;
        this.messageService.add({ severity: 'success', summary: 'Modifié', detail: 'Le visuel a été mis à jour.' });
        this.charger();
      },
      error: (err) => {
        this.enregistrement = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || 'Modification impossible.'
        });
      }
    });
  }

  /** Bascule la visibilité sans ouvrir le formulaire. */
  basculerActif(m: MediaResidenceResponse): void {
    this.api.update(m.id, {
      titre: m.titre, categorie: m.categorie, actif: !m.actif, misEnAvant: m.misEnAvant
    }).subscribe({
      next: () => this.charger(),
      error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Modification impossible.' })
    });
  }

  deplacer(m: MediaResidenceResponse, sens: -1 | 1): void {
    this.api.deplacer(m.id, sens).subscribe({
      next: (liste) => { this.medias = liste; this.media.appliquer(liste); },
      error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Déplacement impossible.' })
    });
  }

  supprimer(m: MediaResidenceResponse): void {
    this.confirmationService.confirm({
      header: 'Supprimer ce visuel ?',
      message: m.heberge
        ? `« ${m.titre} » sera retiré de la galerie et le fichier sera définitivement effacé de l'hébergement. Cette action est irréversible.`
        : `« ${m.titre} » sera retiré de la galerie. L'image d'origine, hébergée ailleurs, n'est pas affectée.`,
      acceptLabel: 'Supprimer', rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.delete(m.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Supprimé', detail: 'Le visuel a été retiré.' });
            this.charger();
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Suppression impossible.' })
        });
      }
    });
  }

  // ==========================================================================
  //  Ajout par adresse
  // ==========================================================================

  ouvrirAjoutUrl(): void {
    this.urlForm.reset({ titre: '', categorie: 'Chambres', url: '', urlVignette: '' });
    this.dialogUrlVisible = true;
  }

  ajouterUrl(): void {
    if (this.urlForm.invalid) return;
    this.enregistrement = true;
    this.api.creerDepuisUrl(this.urlForm.value).subscribe({
      next: () => {
        this.enregistrement = false;
        this.dialogUrlVisible = false;
        this.messageService.add({ severity: 'success', summary: 'Photo ajoutée', detail: 'Le visuel est publié dans la galerie.' });
        this.charger();
      },
      error: (err) => {
        this.enregistrement = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || 'Ajout impossible.'
        });
      }
    });
  }

  isInvalid(form: FormGroup, champ: string): boolean {
    const c = form.get(champ);
    return !!c && c.invalid && (c.dirty || c.touched);
  }
}
