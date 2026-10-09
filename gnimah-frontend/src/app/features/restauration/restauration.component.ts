import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { RestaurationService } from '../../core/services/restauration.service';
import { SejourService } from '../../core/services/sejour.service';
import { AuthService } from '../../core/services/auth.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import {
  ArticleRestaurationResponse, CATEGORIE_RESTO_ICONS, CATEGORIE_RESTO_LABELS,
  CATEGORIE_RESTO_OPTIONS, CommandeResponse, LigneCommandeRequest,
  STATUT_COMMANDE_LABELS, TYPE_COMMANDE_LABELS, TYPE_COMMANDE_OPTIONS
} from '../../core/models/restauration.model';
import { SejourResponse, TYPE_REMISE_OPTIONS } from '../../core/models/sejour.model';
import { modePaiementLabel, modePaiementOptions } from '../../core/models/paiement.model';
import { formatMontant } from '../../core/i18n/devise';

type Onglet = 'SERVICE' | 'ADDITIONS' | 'CARTE';

/** Ligne du panier en cours de composition, avant envoi au backend. */
interface LignePanier {
  articleId: number | null;
  designation: string;
  quantite: number;
  prixUnitaire: number;
}

/**
 * Service du restaurant : prise de commande à partir de la carte, suivi des
 * additions, encaissement ou imputation à une chambre.
 *
 * L'écran « Service » est conçu pour être utilisé debout, au comptoir : la carte
 * à gauche, le panier à droite, tout se fait au clic.
 */
@Component({
  selector: 'app-restauration',
  standalone: false,
  templateUrl: './restauration.component.html',
  styleUrl: './restauration.component.scss'
})
export class RestaurationComponent implements OnInit {
  onglet: Onglet = 'SERVICE';

  carte: ArticleRestaurationResponse[] = [];
  commandes: CommandeResponse[] = [];
  sejoursEnCours: SejourResponse[] = [];

  loading = false;
  saving = false;
  actionId: number | null = null;

  // ---- Prise de commande ----
  panier: LignePanier[] = [];
  categorieActive: string | null = null;
  rechercheCarte = '';
  commandeForm!: FormGroup;

  // ---- Encaissement ----
  showEncaissement = false;
  commandeAEncaisser: CommandeResponse | null = null;
  paiementForm!: FormGroup;

  // ---- Carte ----
  showArticle = false;
  articleEditionId: number | null = null;
  articleForm!: FormGroup;

  categorieOptions = CATEGORIE_RESTO_OPTIONS;
  categorieLabels = CATEGORIE_RESTO_LABELS;
  categorieIcons = CATEGORIE_RESTO_ICONS;
  typeCommandeOptions = TYPE_COMMANDE_OPTIONS;
  typeCommandeLabels = TYPE_COMMANDE_LABELS;
  statutCommandeLabels = STATUT_COMMANDE_LABELS;
  typeRemiseOptions = TYPE_REMISE_OPTIONS;
  modeOptions = modePaiementOptions;
  modeLabels = modePaiementLabel;

  constructor(
    private fb: FormBuilder,
    private restaurationService: RestaurationService,
    private sejourService: SejourService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Restauration', 'Carte, additions et encaissements du restaurant');
    this.initForms();
    this.chargerCarte();
    this.chargerCommandes();
    this.sejourService.findEnCours().subscribe({ next: (d) => { this.sejoursEnCours = d; } });
  }

  private initForms(): void {
    this.commandeForm = this.fb.group({
      type: ['SUR_PLACE', Validators.required],
      tableNumero: [''],
      sejourId: [null],
      imputeSejour: [false],
      remiseType: [null],
      remiseValeur: [null],
      remiseMotif: [''],
      notes: ['']
    });

    this.paiementForm = this.fb.group({
      mode: ['ESPECES', Validators.required],
      referenceTransaction: [''],
      imputeSejour: [false],
      notes: ['']
    });

    this.articleForm = this.fb.group({
      code: [''],
      nom: ['', Validators.required],
      categorie: ['PLAT', Validators.required],
      description: [''],
      prixVente: [null, [Validators.required, Validators.min(1)]],
      coutRevient: [0],
      disponible: [true]
    });
  }

  chargerCarte(): void {
    this.restaurationService.carte(this.onglet === 'CARTE').subscribe({
      next: (d) => { this.carte = d; }
    });
  }

  chargerCommandes(): void {
    this.loading = true;
    this.restaurationService.commandes(0, 60).subscribe({
      next: (page) => { this.commandes = page.content; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  changerOnglet(o: Onglet): void {
    this.onglet = o;
    if (o === 'CARTE' || o === 'SERVICE') this.chargerCarte();
    if (o === 'ADDITIONS') this.chargerCommandes();
  }

  // ------------------------------------------------------------------
  // Carte filtrée
  // ------------------------------------------------------------------

  get categoriesPresentes(): string[] {
    return [...new Set(this.carte.map(a => a.categorie))];
  }

  get carteFiltree(): ArticleRestaurationResponse[] {
    const q = this.rechercheCarte.trim().toLowerCase();
    return this.carte.filter(a =>
      (!this.categorieActive || a.categorie === this.categorieActive) &&
      (!q || a.nom.toLowerCase().includes(q) || (a.code || '').toLowerCase().includes(q)));
  }

  // ------------------------------------------------------------------
  // Panier
  // ------------------------------------------------------------------

  ajouter(a: ArticleRestaurationResponse): void {
    if (!a.disponible) {
      this.messageService.add({
        severity: 'warn', summary: 'Indisponible',
        detail: `${a.nom} n'est pas disponible aujourd'hui`
      });
      return;
    }
    const existante = this.panier.find(l => l.articleId === a.id);
    if (existante) {
      existante.quantite++;
    } else {
      this.panier = [...this.panier, {
        articleId: a.id, designation: a.nom, quantite: 1, prixUnitaire: a.prixVente
      }];
    }
  }

  incrementer(l: LignePanier): void {
    l.quantite++;
  }

  retirer(l: LignePanier): void {
    if (l.quantite > 1) {
      l.quantite--;
    } else {
      this.panier = this.panier.filter(x => x !== l);
    }
  }

  supprimerLigne(l: LignePanier): void {
    this.panier = this.panier.filter(x => x !== l);
  }

  viderPanier(): void {
    this.panier = [];
    this.commandeForm.reset({ type: 'SUR_PLACE', imputeSejour: false });
  }

  get totalBrut(): number {
    return this.panier.reduce((t, l) => t + l.quantite * l.prixUnitaire, 0);
  }

  /** Remise calculée localement, pour que le serveur annonce le net au client. */
  get remisePanier(): number {
    const type = this.commandeForm?.value?.remiseType;
    const valeur = Number(this.commandeForm?.value?.remiseValeur) || 0;
    if (!type || valeur <= 0) return 0;
    const remise = type === 'POURCENTAGE'
      ? Math.round(this.totalBrut * Math.min(valeur, 100) / 100)
      : valeur;
    return Math.min(remise, this.totalBrut);
  }

  get totalNet(): number {
    return this.totalBrut - this.remisePanier;
  }

  get estRoomService(): boolean {
    return this.commandeForm?.value?.type === 'ROOM_SERVICE';
  }

  onTypeCommandeChange(): void {
    if (!this.estRoomService) {
      this.commandeForm.patchValue({ sejourId: null, imputeSejour: false });
    }
  }

  envoyerCommande(): void {
    if (!this.panier.length) {
      this.messageService.add({
        severity: 'warn', summary: 'Panier vide',
        detail: 'Ajoutez au moins un article à l\'addition'
      });
      return;
    }
    const v = this.commandeForm.value;
    if (v.imputeSejour && !v.sejourId) {
      this.messageService.add({
        severity: 'warn', summary: 'Chambre requise',
        detail: 'Choisissez le séjour à débiter pour imputer l\'addition'
      });
      return;
    }

    this.saving = true;
    const lignes: LigneCommandeRequest[] = this.panier.map(l => ({
      articleId: l.articleId,
      designation: l.designation,
      quantite: l.quantite,
      prixUnitaire: l.prixUnitaire
    }));

    this.restaurationService.creerCommande({
      type: v.type,
      tableNumero: v.tableNumero || undefined,
      sejourId: v.sejourId,
      imputeSejour: v.imputeSejour,
      remiseType: v.remiseType,
      remiseValeur: v.remiseValeur,
      remiseMotif: v.remiseMotif || undefined,
      notes: v.notes || undefined,
      lignes
    }).subscribe({
      next: (c) => {
        this.saving = false;
        this.viderPanier();
        this.chargerCommandes();
        this.messageService.add({
          severity: 'success', summary: 'Addition ouverte',
          detail: `${c.numero} — ${formatMontant(c.montantNet)}`
        });
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || "L'addition n'a pas pu être créée"
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Additions
  // ------------------------------------------------------------------

  get additionsOuvertes(): CommandeResponse[] {
    return this.commandes.filter(c => c.statut !== 'PAYEE' && c.statut !== 'ANNULEE');
  }

  statutTone(statut: string): string {
    return {
      OUVERTE: 'amber', ENVOYEE_CUISINE: 'slate', SERVIE: 'gold',
      PAYEE: 'green', ANNULEE: 'gray'
    }[statut] || 'gray';
  }

  marquerServie(c: CommandeResponse): void {
    this.actionId = c.id;
    this.restaurationService.changerStatut(c.id, 'SERVIE').subscribe({
      next: () => { this.actionId = null; this.chargerCommandes(); },
      error: (err) => this.echecAction(err, 'Changement de statut impossible')
    });
  }

  ouvrirEncaissement(c: CommandeResponse): void {
    this.commandeAEncaisser = c;
    this.paiementForm.reset({
      mode: 'ESPECES',
      imputeSejour: c.imputeSejour || (!!c.sejourId && c.type === 'ROOM_SERVICE')
    });
    this.showEncaissement = true;
  }

  encaisser(): void {
    if (!this.commandeAEncaisser || this.paiementForm.invalid) {
      this.paiementForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.restaurationService.encaisser(this.commandeAEncaisser.id, this.paiementForm.value).subscribe({
      next: (c) => {
        this.saving = false;
        this.showEncaissement = false;
        this.messageService.add({
          severity: 'success',
          summary: c.imputeSejour ? 'Portée à la chambre' : 'Addition encaissée',
          detail: `${c.numero} — ${formatMontant(c.montantNet)}`
        });
        this.chargerCommandes();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || "L'encaissement a échoué"
        });
      }
    });
  }

  annulerCommande(c: CommandeResponse): void {
    this.confirmationService.confirm({
      header: 'Annuler l\'addition',
      message: `Annuler ${c.numero} (${formatMontant(c.montantNet)}) ?`,
      icon: 'pi pi-times',
      acceptLabel: 'Annuler l\'addition',
      rejectLabel: 'Retour',
      accept: () => {
        this.actionId = c.id;
        this.restaurationService.annulerCommande(c.id).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({ severity: 'warn', summary: 'Annulée', detail: c.numero });
            this.chargerCommandes();
          },
          error: (err) => this.echecAction(err, 'Annulation impossible')
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Gestion de la carte
  // ------------------------------------------------------------------

  get peutGererCarte(): boolean {
    return this.authService.hasTaskManagementAccess();
  }

  ouvrirArticle(a?: ArticleRestaurationResponse): void {
    this.articleEditionId = a?.id ?? null;
    this.articleForm.reset(a
      ? {
          code: a.code, nom: a.nom, categorie: a.categorie, description: a.description,
          prixVente: a.prixVente, coutRevient: a.coutRevient, disponible: a.disponible
        }
      : { categorie: 'PLAT', coutRevient: 0, disponible: true });
    this.showArticle = true;
  }

  enregistrerArticle(): void {
    if (this.articleForm.invalid) {
      this.articleForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const obs = this.articleEditionId
      ? this.restaurationService.modifierArticle(this.articleEditionId, this.articleForm.value)
      : this.restaurationService.creerArticle(this.articleForm.value);

    obs.subscribe({
      next: (a) => {
        this.saving = false;
        this.showArticle = false;
        this.messageService.add({
          severity: 'success',
          summary: this.articleEditionId ? 'Article modifié' : 'Article ajouté',
          detail: a.nom
        });
        this.chargerCarte();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || "L'enregistrement a échoué"
        });
      }
    });
  }

  basculerDisponibilite(a: ArticleRestaurationResponse): void {
    this.restaurationService.basculerDisponibilite(a.id).subscribe({
      next: (maj) => {
        a.disponible = maj.disponible;
        this.messageService.add({
          severity: 'info',
          summary: maj.disponible ? 'Remis à la carte' : 'Retiré du jour',
          detail: a.nom
        });
      },
      error: (err) => this.echecAction(err, 'Modification impossible')
    });
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  libelleCible(c: CommandeResponse): string {
    if (c.sejourChambre) return `Chambre ${c.sejourChambre}`;
    if (c.tableNumero) return `Table ${c.tableNumero}`;
    if (c.evenementTitre) return c.evenementTitre;
    return this.typeCommandeLabels[c.type] || c.type;
  }

  private echecAction(err: any, defaut: string): void {
    this.actionId = null;
    this.messageService.add({
      severity: 'error', summary: 'Erreur',
      detail: err?.error?.message || defaut
    });
  }

  isInvalid(champ: string, form: FormGroup): boolean {
    const c = form.get(champ);
    return !!(c && c.invalid && c.touched);
  }
}
