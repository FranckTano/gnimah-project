import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ComptabiliteService } from '../../../core/services/comptabilite.service';
import { CaisseService } from '../../../core/services/caisse.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageHeaderService } from '../../../core/services/page-header.service';
import {
  DepenseResponse, FournisseurResponse, FAMILLE_DEPENSE_OPTIONS, FAMILLE_DEPENSE_LABELS,
  FAMILLE_DEPENSE_ICONS, STATUT_DEPENSE_LABELS, TYPE_FOURNISSEUR_OPTIONS
} from '../../../core/models/comptabilite.model';
import { CaisseResponse } from '../../../core/models/caisse.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { modePaiementOptions, modePaiementLabel } from '../../../core/models/paiement.model';
import { Periode, libellePeriode } from '../../../shared/periode';
import { formatMontant } from '../../../core/i18n/devise';

type Onglet = 'DEPENSES' | 'FOURNISSEURS';

/**
 * Saisie et suivi des charges : provisions, restauration, maintenance des
 * chambres, entretien, salaires…
 *
 * Une dépense naît « en attente », se fait valider, puis régler. Seul le
 * règlement sort l'argent d'une caisse — c'est pourquoi les trois gestes sont
 * distincts à l'écran.
 */
@Component({
  selector: 'app-depenses',
  standalone: false,
  templateUrl: './depenses.component.html',
  styleUrl: './depenses.component.scss'
})
export class DepensesComponent implements OnInit {
  onglet: Onglet = 'DEPENSES';

  depenses: DepenseResponse[] = [];
  fournisseurs: FournisseurResponse[] = [];
  caisses: CaisseResponse[] = [];
  chambres: ChambreResponse[] = [];

  loading = false;
  saving = false;
  actionId: number | null = null;
  periode: Periode = { debut: '', fin: '', preset: 'mois-courant' };

  showDialog = false;
  editionId: number | null = null;
  form!: FormGroup;

  showFournisseurDialog = false;
  fournisseurForm!: FormGroup;
  savingFournisseur = false;

  filtreFamille: string | null = null;
  filtreStatut: string | null = null;

  familleOptions = FAMILLE_DEPENSE_OPTIONS;
  familleLabels = FAMILLE_DEPENSE_LABELS;
  familleIcons = FAMILLE_DEPENSE_ICONS;
  statutLabels = STATUT_DEPENSE_LABELS;
  typeFournisseurOptions = TYPE_FOURNISSEUR_OPTIONS;
  modeOptions = modePaiementOptions;
  modeLabels = modePaiementLabel;

  /** Options du filtre : « tous » en tête, typé pour accepter la valeur nulle. */
  familleFiltreOptions: { label: string; value: string | null }[] = [
    { label: 'Tous les postes', value: null },
    ...FAMILLE_DEPENSE_OPTIONS
  ];

  statutOptions: { label: string; value: string | null }[] = [
    { label: 'Tous les statuts', value: null },
    { label: 'En attente',       value: 'EN_ATTENTE' },
    { label: 'Validées',         value: 'VALIDEE' },
    { label: 'Réglées',          value: 'PAYEE' },
    { label: 'Rejetées',         value: 'REJETEE' }
  ];

  constructor(
    private fb: FormBuilder,
    private comptabiliteService: ComptabiliteService,
    private caisseService: CaisseService,
    private chambreService: ChambreService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Dépenses', 'Charges de la résidence et fournisseurs');
    this.initForms();
    this.chargerReferentiels();
  }

  private initForms(): void {
    this.form = this.fb.group({
      libelle: ['', Validators.required],
      montant: [null, [Validators.required, Validators.min(1)]],
      famille: ['PROVISIONS', Validators.required],
      categorie: [''],
      dateDepense: [this.aujourdhui()],
      fournisseurId: [null],
      modeReglement: ['ESPECES'],
      referencePiece: [''],
      chambreId: [null],
      caisseId: [null],
      reglerImmediatement: [false],
      notes: ['']
    });

    this.fournisseurForm = this.fb.group({
      nom: ['', Validators.required],
      type: ['DIVERS'],
      contact: [''],
      telephone: [''],
      email: [''],
      adresse: [''],
      notes: ['']
    });
  }

  private chargerReferentiels(): void {
    this.comptabiliteService.fournisseurs().subscribe({ next: (d) => { this.fournisseurs = d; } });
    this.caisseService.findAll().subscribe({ next: (d) => { this.caisses = d; } });
    this.chambreService.findAll().subscribe({ next: (d) => { this.chambres = d; } });
  }

  onPeriode(p: Periode): void {
    this.periode = p;
    this.charger();
  }

  charger(): void {
    if (!this.periode.debut || !this.periode.fin) return;
    this.loading = true;
    this.comptabiliteService.findDepensesPeriode(this.periode.debut, this.periode.fin).subscribe({
      next: (d) => { this.depenses = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get libelle(): string {
    return libellePeriode(this.periode);
  }

  get depensesFiltrees(): DepenseResponse[] {
    return this.depenses.filter(d =>
      (!this.filtreFamille || d.famille === this.filtreFamille) &&
      (!this.filtreStatut || d.statut === this.filtreStatut));
  }

  get totaux() {
    const l = this.depensesFiltrees;
    return {
      total:      l.reduce((t, d) => t + d.montant, 0),
      payees:     l.filter(d => d.statut === 'PAYEE').reduce((t, d) => t + d.montant, 0),
      enAttente:  l.filter(d => d.statut === 'EN_ATTENTE').reduce((t, d) => t + d.montant, 0),
      nbEnAttente: l.filter(d => d.statut === 'EN_ATTENTE').length
    };
  }

  /** Le règlement et la validation relèvent de l'encadrement, pas de la réception. */
  get peutValider(): boolean {
    return this.authService.hasTaskManagementAccess();
  }

  statutTone(statut: string): string {
    return { EN_ATTENTE: 'amber', VALIDEE: 'slate', PAYEE: 'green', REJETEE: 'red' }[statut] || 'gray';
  }

  // ------------------------------------------------------------------
  // Dépenses
  // ------------------------------------------------------------------

  ouvrirNouvelle(): void {
    this.editionId = null;
    this.form.reset({
      famille: 'PROVISIONS',
      dateDepense: this.aujourdhui(),
      modeReglement: 'ESPECES',
      reglerImmediatement: false
    });
    this.showDialog = true;
  }

  ouvrirEdition(d: DepenseResponse): void {
    this.editionId = d.id;
    this.form.reset({
      libelle: d.libelle,
      montant: d.montant,
      famille: d.famille,
      categorie: d.categorie,
      dateDepense: d.dateDepense,
      fournisseurId: d.fournisseurId,
      modeReglement: d.modeReglement,
      referencePiece: d.referencePiece,
      chambreId: d.chambreId,
      caisseId: d.caisseId,
      reglerImmediatement: false,
      notes: d.notes
    });
    this.showDialog = true;
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const v = this.form.value;
    const request = {
      libelle: v.libelle,
      montant: v.montant,
      famille: v.famille,
      categorie: v.categorie || undefined,
      dateDepense: v.dateDepense || undefined,
      fournisseurId: v.fournisseurId,
      modeReglement: v.modeReglement,
      referencePiece: v.referencePiece || undefined,
      chambreId: v.chambreId,
      caisseId: v.caisseId,
      statut: v.reglerImmediatement && this.peutValider ? 'PAYEE' : undefined,
      notes: v.notes || undefined
    };

    const obs = this.editionId
      ? this.comptabiliteService.modifierDepense(this.editionId, request)
      : this.comptabiliteService.creerDepense(request);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showDialog = false;
        this.messageService.add({
          severity: 'success',
          summary: this.editionId ? 'Dépense modifiée' : 'Dépense enregistrée',
          detail: v.libelle
        });
        this.charger();
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

  valider(d: DepenseResponse): void {
    this.actionId = d.id;
    this.comptabiliteService.validerDepense(d.id).subscribe({
      next: () => {
        this.actionId = null;
        this.messageService.add({ severity: 'success', summary: 'Validée', detail: d.libelle });
        this.charger();
      },
      error: (err) => this.echec(err, 'Validation impossible')
    });
  }

  rejeter(d: DepenseResponse): void {
    this.confirmationService.confirm({
      header: 'Rejeter la dépense',
      message: `Rejeter « ${d.libelle} » (${formatMontant(d.montant)}) ? Elle ne sera pas comptée dans les charges.`,
      icon: 'pi pi-times',
      acceptLabel: 'Rejeter',
      rejectLabel: 'Annuler',
      accept: () => {
        this.actionId = d.id;
        this.comptabiliteService.rejeterDepense(d.id).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({ severity: 'warn', summary: 'Rejetée', detail: d.libelle });
            this.charger();
          },
          error: (err) => this.echec(err, 'Rejet impossible')
        });
      }
    });
  }

  regler(d: DepenseResponse): void {
    const caisse = this.caisses[0];
    this.confirmationService.confirm({
      header: 'Régler la dépense',
      message: `Sortir ${formatMontant(d.montant)} de la caisse pour « ${d.libelle} » ?`
        + (caisse ? ` Le décaissement sera porté sur « ${caisse.nom} ».` : ''),
      icon: 'pi pi-wallet',
      acceptLabel: 'Régler',
      rejectLabel: 'Annuler',
      accept: () => {
        this.actionId = d.id;
        this.comptabiliteService.reglerDepense(d.id, d.caisseId ?? null).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({
              severity: 'success', summary: 'Dépense réglée',
              detail: `${formatMontant(d.montant)} décaissés`
            });
            this.charger();
          },
          error: (err) => this.echec(err, 'Règlement impossible')
        });
      }
    });
  }

  supprimer(d: DepenseResponse): void {
    this.confirmationService.confirm({
      header: 'Supprimer la dépense',
      message: `Supprimer définitivement « ${d.libelle} » ?`,
      icon: 'pi pi-trash',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.actionId = d.id;
        this.comptabiliteService.supprimerDepense(d.id).subscribe({
          next: () => {
            this.actionId = null;
            this.messageService.add({ severity: 'info', summary: 'Supprimée', detail: d.libelle });
            this.charger();
          },
          error: (err) => this.echec(err, 'Suppression impossible')
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Fournisseurs
  // ------------------------------------------------------------------

  ouvrirFournisseur(): void {
    this.fournisseurForm.reset({ type: 'DIVERS' });
    this.showFournisseurDialog = true;
  }

  enregistrerFournisseur(): void {
    if (this.fournisseurForm.invalid) {
      this.fournisseurForm.markAllAsTouched();
      return;
    }
    this.savingFournisseur = true;
    this.comptabiliteService.creerFournisseur(this.fournisseurForm.value).subscribe({
      next: (f) => {
        this.savingFournisseur = false;
        this.showFournisseurDialog = false;
        this.fournisseurs = [...this.fournisseurs, f].sort((a, b) => a.nom.localeCompare(b.nom));
        this.messageService.add({ severity: 'success', summary: 'Fournisseur créé', detail: f.nom });
      },
      error: (err) => {
        this.savingFournisseur = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || 'Création impossible'
        });
      }
    });
  }

  desactiverFournisseur(f: FournisseurResponse): void {
    this.confirmationService.confirm({
      header: 'Désactiver le fournisseur',
      message: `Retirer « ${f.nom} » de la liste ? Les dépenses déjà saisies restent inchangées.`,
      icon: 'pi pi-ban',
      acceptLabel: 'Désactiver',
      rejectLabel: 'Annuler',
      accept: () => {
        this.comptabiliteService.desactiverFournisseur(f.id).subscribe({
          next: () => {
            this.fournisseurs = this.fournisseurs.filter(x => x.id !== f.id);
            this.messageService.add({ severity: 'info', summary: 'Désactivé', detail: f.nom });
          },
          error: (err) => this.echec(err, 'Désactivation impossible')
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  private echec(err: any, defaut: string): void {
    this.actionId = null;
    this.messageService.add({
      severity: 'error', summary: 'Erreur',
      detail: err?.error?.message || defaut
    });
  }

  private aujourdhui(): string {
    const d = new Date();
    const p = (n: number) => `${n}`.padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  }

  isInvalid(champ: string, form: FormGroup = this.form): boolean {
    const c = form.get(champ);
    return !!(c && c.invalid && c.touched);
  }
}
