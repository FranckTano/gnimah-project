import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StockService } from '../../core/services/stock.service';
import { ComptabiliteService } from '../../core/services/comptabilite.service';
import { CaisseService } from '../../core/services/caisse.service';
import { ChambreService } from '../../core/services/chambre.service';
import { AuthService } from '../../core/services/auth.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import {
  ArticleStockResponse, CATEGORIE_STOCK_ICONS, CATEGORIE_STOCK_LABELS, CATEGORIE_STOCK_OPTIONS,
  MouvementStockResponse, TYPE_MOUVEMENT_STOCK_LABELS, TYPE_MOUVEMENT_STOCK_OPTIONS,
  TYPES_ENTREE_STOCK, UNITE_LABELS, UNITE_OPTIONS
} from '../../core/models/stock.model';
import { FournisseurResponse } from '../../core/models/comptabilite.model';
import { CaisseResponse } from '../../core/models/caisse.model';
import { ChambreResponse } from '../../core/models/chambre.model';
import { modePaiementOptions } from '../../core/models/paiement.model';

type Onglet = 'INVENTAIRE' | 'MOUVEMENTS';

/**
 * Provisions et matériel de la résidence : denrées de cuisine, boissons,
 * produits d'entretien, linge, petit équipement des chambres.
 *
 * Un achat saisi ici peut générer d'un seul geste l'entrée de stock, la dépense
 * comptable et le décaissement en caisse — c'est le sens de l'option « générer
 * la dépense » : éviter la triple saisie et les oublis qui vont avec.
 */
@Component({
  selector: 'app-stock',
  standalone: false,
  templateUrl: './stock.component.html',
  styleUrl: './stock.component.scss'
})
export class StockComponent implements OnInit {
  onglet: Onglet = 'INVENTAIRE';

  articles: ArticleStockResponse[] = [];
  mouvements: MouvementStockResponse[] = [];
  fournisseurs: FournisseurResponse[] = [];
  caisses: CaisseResponse[] = [];
  chambres: ChambreResponse[] = [];

  loading = false;
  saving = false;
  valeurTotale = 0;

  filtreCategorie: string | null = null;
  recherche = '';
  alertesSeules = false;

  showArticle = false;
  articleEditionId: number | null = null;
  articleForm!: FormGroup;

  showMouvement = false;
  articleCible: ArticleStockResponse | null = null;
  mouvementForm!: FormGroup;

  showHistorique = false;
  historique: MouvementStockResponse[] = [];

  categorieOptions = CATEGORIE_STOCK_OPTIONS;
  /** Options du filtre : « toutes » en tête, typé pour accepter la valeur nulle. */
  categorieFiltreOptions: { label: string; value: string | null }[] = [
    { label: 'Toutes catégories', value: null },
    ...CATEGORIE_STOCK_OPTIONS
  ];
  categorieLabels = CATEGORIE_STOCK_LABELS;
  categorieIcons = CATEGORIE_STOCK_ICONS;
  uniteOptions = UNITE_OPTIONS;
  uniteLabels = UNITE_LABELS;
  typeMouvementOptions = TYPE_MOUVEMENT_STOCK_OPTIONS;
  typeMouvementLabels = TYPE_MOUVEMENT_STOCK_LABELS;
  modeOptions = modePaiementOptions;

  constructor(
    private fb: FormBuilder,
    private stockService: StockService,
    private comptabiliteService: ComptabiliteService,
    private caisseService: CaisseService,
    private chambreService: ChambreService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Stock & provisions', 'Inventaire, approvisionnements et consommations');
    this.initForms();
    this.charger();
    this.comptabiliteService.fournisseurs().subscribe({ next: (d) => { this.fournisseurs = d; } });
    this.caisseService.findAll().subscribe({ next: (d) => { this.caisses = d; } });
    this.chambreService.findAll().subscribe({ next: (d) => { this.chambres = d; } });
  }

  private initForms(): void {
    this.articleForm = this.fb.group({
      code: [''],
      designation: ['', Validators.required],
      categorie: ['PROVISION_ALIMENTAIRE', Validators.required],
      unite: ['PIECE'],
      quantite: [0],
      seuilAlerte: [0],
      prixUnitaire: [0],
      fournisseurId: [null],
      emplacement: ['']
    });

    this.mouvementForm = this.fb.group({
      type: ['ACHAT', Validators.required],
      quantite: [null, [Validators.required, Validators.min(0.01)]],
      prixUnitaire: [null],
      fournisseurId: [null],
      chambreId: [null],
      genererDepense: [true],
      caisseId: [null],
      modeReglement: ['ESPECES'],
      motif: ['']
    });
  }

  charger(): void {
    this.loading = true;
    this.stockService.articles().subscribe({
      next: (d) => { this.articles = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
    this.stockService.valeur().subscribe({ next: (r) => { this.valeurTotale = r.valeur; } });
  }

  chargerMouvements(): void {
    this.loading = true;
    this.stockService.journal(0, 80).subscribe({
      next: (page) => { this.mouvements = page.content; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  changerOnglet(o: Onglet): void {
    this.onglet = o;
    if (o === 'MOUVEMENTS') this.chargerMouvements();
    else this.charger();
  }

  // ------------------------------------------------------------------
  // Filtres et agrégats
  // ------------------------------------------------------------------

  get articlesFiltres(): ArticleStockResponse[] {
    const q = this.recherche.trim().toLowerCase();
    return this.articles.filter(a =>
      (!this.filtreCategorie || a.categorie === this.filtreCategorie) &&
      (!this.alertesSeules || a.enAlerte) &&
      (!q || a.designation.toLowerCase().includes(q) || (a.code || '').toLowerCase().includes(q)));
  }

  get nbAlertes(): number {
    return this.articles.filter(a => a.enAlerte).length;
  }

  get categoriesPresentes(): string[] {
    return [...new Set(this.articles.map(a => a.categorie))];
  }

  get peutGerer(): boolean {
    return this.authService.hasTaskManagementAccess();
  }

  estEntree(type: string): boolean {
    return TYPES_ENTREE_STOCK.includes(type);
  }

  /** Remplissage de la jauge, plafonné : un stock à 300 % du seuil reste plein. */
  jauge(a: ArticleStockResponse): number {
    if (!a.seuilAlerte || a.seuilAlerte <= 0) return a.quantite > 0 ? 100 : 0;
    return Math.min(100, Math.round((a.quantite / (a.seuilAlerte * 2)) * 100));
  }

  jaugeTon(a: ArticleStockResponse): string {
    if (a.quantite <= 0) return '#d0455a';
    if (a.enAlerte) return '#c9922f';
    return '#2f9e6f';
  }

  // ------------------------------------------------------------------
  // Article
  // ------------------------------------------------------------------

  ouvrirArticle(a?: ArticleStockResponse): void {
    this.articleEditionId = a?.id ?? null;
    this.articleForm.reset(a
      ? {
          code: a.code, designation: a.designation, categorie: a.categorie, unite: a.unite,
          quantite: a.quantite, seuilAlerte: a.seuilAlerte, prixUnitaire: a.prixUnitaire,
          fournisseurId: a.fournisseurId, emplacement: a.emplacement
        }
      : { categorie: 'PROVISION_ALIMENTAIRE', unite: 'PIECE', quantite: 0, seuilAlerte: 0, prixUnitaire: 0 });
    this.showArticle = true;
  }

  enregistrerArticle(): void {
    if (this.articleForm.invalid) {
      this.articleForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const obs = this.articleEditionId
      ? this.stockService.modifier(this.articleEditionId, this.articleForm.value)
      : this.stockService.creer(this.articleForm.value);

    obs.subscribe({
      next: (a) => {
        this.saving = false;
        this.showArticle = false;
        this.messageService.add({
          severity: 'success',
          summary: this.articleEditionId ? 'Article modifié' : 'Article créé',
          detail: a.designation
        });
        this.charger();
      },
      error: (err) => this.echec(err, "L'enregistrement a échoué")
    });
  }

  desactiver(a: ArticleStockResponse): void {
    this.confirmationService.confirm({
      header: 'Retirer de l\'inventaire',
      message: `Retirer « ${a.designation} » de l'inventaire ? L'historique de ses mouvements est conservé.`,
      icon: 'pi pi-ban',
      acceptLabel: 'Retirer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.stockService.desactiver(a.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'info', summary: 'Retiré', detail: a.designation });
            this.charger();
          },
          error: (err) => this.echec(err, 'Retrait impossible')
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Mouvement
  // ------------------------------------------------------------------

  ouvrirMouvement(a: ArticleStockResponse, type = 'ACHAT'): void {
    this.articleCible = a;
    this.mouvementForm.reset({
      type,
      quantite: null,
      prixUnitaire: a.prixUnitaire,
      fournisseurId: a.fournisseurId,
      chambreId: null,
      genererDepense: type === 'ACHAT',
      caisseId: null,
      modeReglement: 'ESPECES',
      motif: ''
    });
    this.showMouvement = true;
  }

  get mouvementEstEntree(): boolean {
    return this.estEntree(this.mouvementForm?.value?.type);
  }

  get mouvementEstAchat(): boolean {
    return this.mouvementForm?.value?.type === 'ACHAT';
  }

  get mouvementVersChambre(): boolean {
    return this.mouvementForm?.value?.type === 'CONSOMMATION_CHAMBRE';
  }

  get montantMouvement(): number {
    const q = Number(this.mouvementForm?.value?.quantite) || 0;
    const p = Number(this.mouvementForm?.value?.prixUnitaire) || 0;
    return q * p;
  }

  onTypeMouvementChange(): void {
    this.mouvementForm.patchValue({ genererDepense: this.mouvementEstAchat });
  }

  enregistrerMouvement(): void {
    if (!this.articleCible || this.mouvementForm.invalid) {
      this.mouvementForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.stockService.enregistrerMouvement({
      articleId: this.articleCible.id,
      ...this.mouvementForm.value
    }).subscribe({
      next: (m) => {
        this.saving = false;
        this.showMouvement = false;
        this.messageService.add({
          severity: 'success',
          summary: m.sens === 'ENTREE' ? 'Entrée enregistrée' : 'Sortie enregistrée',
          detail: `${m.articleDesignation} — stock : ${m.quantiteApres} ${this.uniteLabels[m.unite] || m.unite}`
        });
        this.charger();
      },
      error: (err) => this.echec(err, "Le mouvement n'a pas pu être enregistré")
    });
  }

  voirHistorique(a: ArticleStockResponse): void {
    this.articleCible = a;
    this.historique = [];
    this.showHistorique = true;
    this.stockService.historiqueArticle(a.id).subscribe({
      next: (m) => { this.historique = m; }
    });
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  private echec(err: any, defaut: string): void {
    this.saving = false;
    this.messageService.add({
      severity: 'error', summary: 'Erreur',
      detail: err?.error?.message || defaut
    });
  }

  isInvalid(champ: string, form: FormGroup): boolean {
    const c = form.get(champ);
    return !!(c && c.invalid && c.touched);
  }

  formatMontant(v: number | null | undefined): string {
    return new Intl.NumberFormat('fr-CI', {
      style: 'currency', currency: 'XOF', maximumFractionDigits: 0
    }).format(v || 0);
  }

  formatQte(v: number | null | undefined): string {
    const n = v || 0;
    return Number.isInteger(n) ? `${n}` : n.toFixed(2);
  }
}
