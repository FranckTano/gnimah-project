import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { VersementService } from '../../../core/services/versement.service';
import { CaisseService } from '../../../core/services/caisse.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageHeaderService } from '../../../core/services/page-header.service';
import {
  ACTION_VERSEMENT_ICONS, ACTION_VERSEMENT_LABELS, CompteFinancierResponse,
  PERIODICITE_LABELS, PERIODICITE_OPTIONS, RecetteAVerserResponse,
  STATUT_VERSEMENT_LABELS, TYPE_COMPTE_OPTIONS, VersementResponse
} from '../../../core/models/versement.model';
import { CaisseResponse } from '../../../core/models/caisse.model';
import { modePaiementLabel, modePaiementOptions } from '../../../core/models/paiement.model';
import { iso } from '../../../shared/periode';
import { formatMontant } from '../../../core/i18n/devise';

type Onglet = 'VERSEMENTS' | 'COMPTES';

/**
 * Versement de la recette d'une caisse vers un compte bancaire ou un numéro
 * mobile money.
 *
 * L'écran suit les trois temps du geste réel : la caissière prépare pour une
 * période (journée, semaine ou mois), enregistre le dépôt effectif avec sa date,
 * puis la direction confirme au vu du reçu. Chaque étape s'inscrit dans un
 * journal d'actions consultable sur la fiche du versement.
 */
@Component({
  selector: 'app-versements',
  standalone: false,
  templateUrl: './versements.component.html',
  styleUrl: './versements.component.scss'
})
export class VersementsComponent implements OnInit {
  onglet: Onglet = 'VERSEMENTS';

  versements: VersementResponse[] = [];
  comptes: CompteFinancierResponse[] = [];
  caisses: CaisseResponse[] = [];
  recette: RecetteAVerserResponse | null = null;

  loading = false;
  saving = false;
  actionId: number | null = null;

  showPreparation = false;
  showDetail = false;
  showCompte = false;
  selection: VersementResponse | null = null;

  form!: FormGroup;
  compteForm!: FormGroup;

  periodiciteOptions = PERIODICITE_OPTIONS;
  periodiciteLabels = PERIODICITE_LABELS;
  statutLabels = STATUT_VERSEMENT_LABELS;
  actionLabels = ACTION_VERSEMENT_LABELS;
  actionIcons = ACTION_VERSEMENT_ICONS;
  typeCompteOptions = TYPE_COMPTE_OPTIONS;
  modeOptions = modePaiementOptions;
  modeLabels = modePaiementLabel;

  filtreStatut: string | null = null;

  statutOptions = [
    { label: 'Tous les statuts', value: null },
    { label: 'Préparés',  value: 'PREPARE' },
    { label: 'Déposés',   value: 'EFFECTUE' },
    { label: 'Confirmés', value: 'CONFIRME' },
    { label: 'Annulés',   value: 'ANNULE' }
  ];

  constructor(
    private fb: FormBuilder,
    private versementService: VersementService,
    private caisseService: CaisseService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Versements', 'Dépôts de recette en banque et mobile money');
    this.initForms();
    this.charger();
    this.caisseService.findAll().subscribe({ next: (d) => { this.caisses = d; } });
    this.chargerComptes();
  }

  private initForms(): void {
    this.form = this.fb.group({
      caisseId: [null, Validators.required],
      compteId: [null, Validators.required],
      periodicite: ['JOURNALIER', Validators.required],
      periodeDebut: [iso(new Date())],
      periodeFin: [iso(new Date())],
      montant: [null, [Validators.required, Validators.min(1)]],
      dateVersement: [iso(new Date()), Validators.required],
      mode: ['ESPECES'],
      referenceTransaction: [''],
      notes: ['']
    });

    this.compteForm = this.fb.group({
      libelle: ['', Validators.required],
      type: ['BANQUE', Validators.required],
      etablissement: [''],
      numeroCompte: [''],
      numeroTelephone: [''],
      titulaire: ['Résidence Hôtel GNIMAH'],
      notes: ['']
    });
  }

  charger(): void {
    this.loading = true;
    this.versementService.findAll(0, 60).subscribe({
      next: (page) => { this.versements = page.content; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  private chargerComptes(): void {
    this.versementService.comptes().subscribe({ next: (d) => { this.comptes = d; } });
  }

  get versementsFiltres(): VersementResponse[] {
    return this.filtreStatut
      ? this.versements.filter(v => v.statut === this.filtreStatut)
      : this.versements;
  }

  get totaux() {
    const l = this.versementsFiltres.filter(v => v.statut !== 'ANNULE');
    return {
      total:     l.reduce((t, v) => t + v.montant, 0),
      aConfirmer: this.versements.filter(v => v.statut === 'EFFECTUE').length,
      prepares:   this.versements.filter(v => v.statut === 'PREPARE').length
    };
  }

  /** La confirmation est un contrôle : elle ne peut pas revenir à qui a déposé. */
  get peutConfirmer(): boolean {
    return this.authService.hasTaskManagementAccess();
  }

  statutTone(statut: string): string {
    return { PREPARE: 'amber', EFFECTUE: 'slate', CONFIRME: 'green', ANNULE: 'gray' }[statut] || 'gray';
  }

  // ------------------------------------------------------------------
  // Préparation
  // ------------------------------------------------------------------

  ouvrirPreparation(): void {
    const aujourdhui = iso(new Date());
    this.recette = null;
    this.form.reset({
      caisseId: this.caisses[0]?.id ?? null,
      compteId: this.comptes[0]?.id ?? null,
      periodicite: 'JOURNALIER',
      periodeDebut: aujourdhui,
      periodeFin: aujourdhui,
      dateVersement: aujourdhui,
      mode: 'ESPECES'
    });
    this.showPreparation = true;
    this.calculerRecette();
  }

  /** Aligne les bornes de période sur la périodicité choisie. */
  onPeriodiciteChange(): void {
    const fin = new Date(this.form.value.dateVersement || new Date());
    const debut = new Date(fin);
    switch (this.form.value.periodicite) {
      case 'HEBDOMADAIRE': debut.setDate(debut.getDate() - 6); break;
      case 'MENSUEL':      debut.setDate(1); break;
      default:             break;
    }
    this.form.patchValue({ periodeDebut: iso(debut), periodeFin: iso(fin) });
    this.calculerRecette();
  }

  /**
   * Interroge le backend sur ce qui reste à verser pour la période, et
   * pré-remplit le montant. La caissière garde la main : elle peut verser moins
   * si elle conserve un fond de roulement.
   */
  calculerRecette(): void {
    const { caisseId, periodeDebut, periodeFin } = this.form.value;
    if (!caisseId || !periodeDebut || !periodeFin) return;
    this.versementService.recetteAVerser(caisseId, periodeDebut, periodeFin).subscribe({
      next: (r) => {
        this.recette = r;
        if (!this.form.value.montant && r.resteAVerser > 0) {
          this.form.patchValue({ montant: r.resteAVerser });
        }
      },
      error: () => { this.recette = null; }
    });
  }

  preparer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.versementService.preparer(this.form.value).subscribe({
      next: (v) => {
        this.saving = false;
        this.showPreparation = false;
        this.messageService.add({
          severity: 'success', summary: 'Versement préparé',
          detail: `${v.numero} — ${formatMontant(v.montant)}`
        });
        this.charger();
      },
      error: (err) => this.echec(err, 'La préparation a échoué')
    });
  }

  // ------------------------------------------------------------------
  // Cycle de vie
  // ------------------------------------------------------------------

  deposer(v: VersementResponse): void {
    this.confirmationService.confirm({
      header: 'Confirmer le dépôt',
      message: `Déposer ${formatMontant(v.montant)} sur « ${v.compteLibelle} » ? `
        + `Le montant sort immédiatement de ${v.caisseNom}.`,
      icon: 'pi pi-building',
      acceptLabel: 'Dépôt effectué',
      rejectLabel: 'Annuler',
      accept: () => {
        this.actionId = v.id;
        this.versementService.deposer(v.id, { commentaire: 'Dépôt effectué' }).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({
              severity: 'success', summary: 'Dépôt enregistré',
              detail: `${v.numero} en attente de confirmation`
            });
            this.charger();
          },
          error: (err) => this.echecAction(err, 'Le dépôt a échoué')
        });
      }
    });
  }

  confirmer(v: VersementResponse): void {
    this.confirmationService.confirm({
      header: 'Confirmer le versement',
      message: `Confirmer la réception de ${formatMontant(v.montant)} sur « ${v.compteLibelle} » ? `
        + `À faire au vu du reçu de la banque ou du message de l'opérateur.`,
      icon: 'pi pi-check-circle',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Plus tard',
      accept: () => {
        this.actionId = v.id;
        this.versementService.confirmer(v.id, { commentaire: 'Justificatif vérifié' }).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({ severity: 'success', summary: 'Versement confirmé', detail: v.numero });
            this.charger();
            this.chargerComptes();
          },
          error: (err) => this.echecAction(err, 'La confirmation a échoué')
        });
      }
    });
  }

  annuler(v: VersementResponse): void {
    this.confirmationService.confirm({
      header: 'Annuler le versement',
      message: v.statut === 'PREPARE'
        ? `Annuler le versement ${v.numero} ?`
        : `Annuler ${v.numero} ? Les ${formatMontant(v.montant)} reviendront en caisse par un mouvement inverse.`,
      icon: 'pi pi-times-circle',
      acceptLabel: 'Annuler le versement',
      rejectLabel: 'Retour',
      accept: () => {
        this.actionId = v.id;
        this.versementService.annuler(v.id, { commentaire: 'Annulation depuis la caisse' }).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({ severity: 'warn', summary: 'Versement annulé', detail: v.numero });
            this.charger();
            this.chargerComptes();
          },
          error: (err) => this.echecAction(err, "L'annulation a échoué")
        });
      }
    });
  }

  voirDetail(v: VersementResponse): void {
    this.versementService.getById(v.id).subscribe({
      next: (d) => { this.selection = d; this.showDetail = true; },
      error: () => {
        this.selection = v;
        this.showDetail = true;
      }
    });
  }

  // ------------------------------------------------------------------
  // Comptes de destination
  // ------------------------------------------------------------------

  ouvrirCompte(): void {
    this.compteForm.reset({ type: 'BANQUE', titulaire: 'Résidence Hôtel GNIMAH' });
    this.showCompte = true;
  }

  get compteEstMobile(): boolean {
    return this.compteForm?.value?.type === 'MOBILE_MONEY';
  }

  enregistrerCompte(): void {
    if (this.compteForm.invalid) {
      this.compteForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.versementService.creerCompte(this.compteForm.value).subscribe({
      next: (c) => {
        this.saving = false;
        this.showCompte = false;
        this.comptes = [...this.comptes, c];
        this.messageService.add({ severity: 'success', summary: 'Compte ajouté', detail: c.libelle });
      },
      error: (err) => this.echec(err, "L'ajout du compte a échoué")
    });
  }

  desactiverCompte(c: CompteFinancierResponse): void {
    this.confirmationService.confirm({
      header: 'Désactiver le compte',
      message: `Retirer « ${c.libelle} » des destinations de versement ?`,
      icon: 'pi pi-ban',
      acceptLabel: 'Désactiver',
      rejectLabel: 'Annuler',
      accept: () => {
        this.versementService.desactiverCompte(c.id).subscribe({
          next: () => {
            this.comptes = this.comptes.filter(x => x.id !== c.id);
            this.messageService.add({ severity: 'info', summary: 'Désactivé', detail: c.libelle });
          },
          error: (err) => this.echecAction(err, 'Désactivation impossible')
        });
      }
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
