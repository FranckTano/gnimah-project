import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { IncidentService } from '../../../core/services/incident.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { SejourService } from '../../../core/services/sejour.service';
import { StockService } from '../../../core/services/stock.service';
import { CaisseService } from '../../../core/services/caisse.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageHeaderService } from '../../../core/services/page-header.service';
import {
  GRAVITE_LABELS, GRAVITE_OPTIONS, GRAVITE_TONES, IncidentResponse, RESOLUTION_OPTIONS,
  STATUT_INCIDENT_LABELS, STATUT_INCIDENT_TONES, TYPE_INCIDENT_ICONS,
  TYPE_INCIDENT_LABELS, TYPE_INCIDENT_OPTIONS
} from '../../../core/models/incident.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { SejourResponse } from '../../../core/models/sejour.model';
import { ArticleStockResponse } from '../../../core/models/stock.model';
import { CaisseResponse } from '../../../core/models/caisse.model';
import { FAMILLE_DEPENSE_OPTIONS } from '../../../core/models/comptabilite.model';
import { formatMontant } from '../../../core/i18n/devise';

/**
 * Déclaration et suivi du matériel défectueux relevé en chambre après le départ
 * des clients.
 *
 * L'agent d'entretien constate et déclare — c'est tout ce qu'on lui demande.
 * Le chiffrage et la décision d'imputer au client ou à la résidence
 * appartiennent à l'encadrement, dans un second temps.
 */
@Component({
  selector: 'app-incidents',
  standalone: false,
  templateUrl: './incidents.component.html',
  styleUrl: './incidents.component.scss'
})
export class IncidentsComponent implements OnInit {
  incidents: IncidentResponse[] = [];
  chambres: ChambreResponse[] = [];
  sejours: SejourResponse[] = [];
  articlesStock: ArticleStockResponse[] = [];
  caisses: CaisseResponse[] = [];

  loading = false;
  saving = false;
  actionId: number | null = null;

  filtreStatut: string | null = null;
  filtreGravite: string | null = null;

  showDeclaration = false;
  form!: FormGroup;

  showResolution = false;
  incidentCible: IncidentResponse | null = null;
  resolutionForm!: FormGroup;

  typeOptions = TYPE_INCIDENT_OPTIONS;
  typeLabels = TYPE_INCIDENT_LABELS;
  typeIcons = TYPE_INCIDENT_ICONS;
  graviteOptions = GRAVITE_OPTIONS;
  graviteLabels = GRAVITE_LABELS;
  graviteTones = GRAVITE_TONES;
  statutLabels = STATUT_INCIDENT_LABELS;
  statutTones = STATUT_INCIDENT_TONES;
  resolutionOptions = RESOLUTION_OPTIONS;
  familleOptions = FAMILLE_DEPENSE_OPTIONS;

  graviteFiltreOptions: { label: string; value: string | null }[] = [
    { label: 'Toutes gravités', value: null },
    ...GRAVITE_OPTIONS
  ];

  statutOptions: { label: string; value: string | null }[] = [
    { label: 'Tous les statuts', value: null },
    { label: 'Déclarés',          value: 'DECLARE' },
    { label: 'En cours',          value: 'EN_COURS' },
    { label: 'Réparés',           value: 'REPARE' },
    { label: 'Remplacés',         value: 'REMPLACE' },
    { label: 'Facturés au client', value: 'IMPUTE_CLIENT' },
    { label: 'Classés sans suite', value: 'CLASSE_SANS_SUITE' }
  ];

  constructor(
    private fb: FormBuilder,
    private incidentService: IncidentService,
    private chambreService: ChambreService,
    private sejourService: SejourService,
    private stockService: StockService,
    private caisseService: CaisseService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Matériel & incidents',
      'Objets défectueux, manquants ou détériorés relevés en chambre');
    this.initForms();
    this.charger();
    this.chambreService.findAll().subscribe({ next: (d) => { this.chambres = d; } });
    this.sejourService.findEnCours().subscribe({ next: (d) => { this.sejours = d; } });
    this.stockService.articles().subscribe({ next: (d) => { this.articlesStock = d; } });
    this.caisseService.findAll().subscribe({ next: (d) => { this.caisses = d; } });
  }

  private initForms(): void {
    this.form = this.fb.group({
      chambreId: [null, Validators.required],
      sejourId: [null],
      type: ['OBJET_CASSE', Validators.required],
      designation: ['', Validators.required],
      articleStockId: [null],
      quantite: [1, [Validators.min(1)]],
      gravite: ['MINEUR'],
      description: [''],
      coutEstime: [null],
      imputeClient: [false],
      bloquerChambre: [false],
      notes: ['']
    });

    this.resolutionForm = this.fb.group({
      statut: ['REPARE', Validators.required],
      coutReel: [null],
      imputeClient: [false],
      familleDepense: ['MAINTENANCE'],
      caisseId: [null],
      libererChambre: [true],
      notes: ['']
    });
  }

  charger(): void {
    this.loading = true;
    this.incidentService.findAll().subscribe({
      next: (d) => { this.incidents = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ------------------------------------------------------------------
  // Filtres et agrégats
  // ------------------------------------------------------------------

  get incidentsFiltres(): IncidentResponse[] {
    return this.incidents.filter(i =>
      (!this.filtreStatut || i.statut === this.filtreStatut) &&
      (!this.filtreGravite || i.gravite === this.filtreGravite));
  }

  get stats() {
    return {
      ouverts:  this.incidents.filter(i => i.statut === 'DECLARE' || i.statut === 'EN_COURS').length,
      majeurs:  this.incidents.filter(i => i.gravite === 'MAJEUR' &&
                  (i.statut === 'DECLARE' || i.statut === 'EN_COURS')).length,
      coutTotal: this.incidents.reduce((t, i) => t + i.coutReel, 0),
      factures:  this.incidents.filter(i => i.imputeClient).reduce((t, i) => t + i.coutReel, 0)
    };
  }

  /** La résolution engage de l'argent : elle reste à l'encadrement. */
  get peutResoudre(): boolean {
    return this.authService.hasTaskManagementAccess();
  }

  /** Séjours en cours de la chambre choisie, pour rattacher le dégât au bon client. */
  get sejoursChambre(): SejourResponse[] {
    const chambreId = this.form?.value?.chambreId;
    if (!chambreId) return this.sejours;
    return this.sejours.filter(s => s.chambreId === chambreId);
  }

  // ------------------------------------------------------------------
  // Déclaration
  // ------------------------------------------------------------------

  ouvrirDeclaration(): void {
    this.form.reset({
      type: 'OBJET_CASSE', quantite: 1, gravite: 'MINEUR',
      imputeClient: false, bloquerChambre: false
    });
    this.showDeclaration = true;
  }

  /** Sélectionner un article de stock pré-remplit désignation et coût estimé. */
  onArticleStockChange(): void {
    const id = this.form.value.articleStockId;
    if (!id) return;
    const article = this.articlesStock.find(a => a.id === id);
    if (!article) return;
    const patch: any = { designation: article.designation };
    if (!this.form.value.coutEstime) {
      patch.coutEstime = article.prixUnitaire * (this.form.value.quantite || 1);
    }
    this.form.patchValue(patch);
  }

  declarer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.messageService.add({
        severity: 'warn', summary: 'Champs requis',
        detail: 'La chambre, le type et la désignation sont obligatoires'
      });
      return;
    }
    this.saving = true;
    this.incidentService.declarer(this.form.value).subscribe({
      next: (i) => {
        this.saving = false;
        this.showDeclaration = false;
        this.messageService.add({
          severity: 'success', summary: 'Incident déclaré',
          detail: `${i.numero} — chambre ${i.chambreNumero}`
        });
        this.charger();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || 'La déclaration a échoué'
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Traitement
  // ------------------------------------------------------------------

  prendreEnCharge(i: IncidentResponse): void {
    this.actionId = i.id;
    this.incidentService.prendreEnCharge(i.id).subscribe({
      next: () => {
        this.actionId = null;
        this.messageService.add({ severity: 'info', summary: 'Pris en charge', detail: i.numero });
        this.charger();
      },
      error: (err) => this.echecAction(err, 'Prise en charge impossible')
    });
  }

  ouvrirResolution(i: IncidentResponse): void {
    this.incidentCible = i;
    this.resolutionForm.reset({
      statut: 'REPARE',
      coutReel: i.coutEstime > 0 ? i.coutEstime : null,
      imputeClient: i.imputeClient,
      familleDepense: 'MAINTENANCE',
      caisseId: null,
      libererChambre: true,
      notes: ''
    });
    this.showResolution = true;
  }

  get resolutionImpute(): boolean {
    return !!this.resolutionForm?.value?.imputeClient;
  }

  get resolutionSansSuite(): boolean {
    return this.resolutionForm?.value?.statut === 'CLASSE_SANS_SUITE';
  }

  /** Un dégât ne peut être facturé qu'à un client identifié par son séjour. */
  get peutImputer(): boolean {
    return !!this.incidentCible?.sejourId;
  }

  resoudre(): void {
    if (!this.incidentCible || this.resolutionForm.invalid) {
      this.resolutionForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.incidentService.resoudre(this.incidentCible.id, this.resolutionForm.value).subscribe({
      next: (i) => {
        this.saving = false;
        this.showResolution = false;
        this.messageService.add({
          severity: 'success',
          summary: i.imputeClient ? 'Facturé au client' : 'Incident clôturé',
          detail: i.imputeClient
            ? `${formatMontant(i.coutReel)} ajoutés à la note de ${i.clientNom}`
            : `${i.numero} — ${this.statutLabels[i.statut]}`
        });
        this.charger();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || 'La clôture a échoué'
        });
      }
    });
  }

  supprimer(i: IncidentResponse): void {
    this.confirmationService.confirm({
      header: 'Supprimer l\'incident',
      message: `Supprimer ${i.numero} (${i.designation}) ?`,
      icon: 'pi pi-trash',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.actionId = i.id;
        this.incidentService.supprimer(i.id).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({ severity: 'info', summary: 'Supprimé', detail: i.numero });
            this.charger();
          },
          error: (err) => this.echecAction(err, 'Suppression impossible')
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  estOuvert(i: IncidentResponse): boolean {
    return i.statut === 'DECLARE' || i.statut === 'EN_COURS';
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
