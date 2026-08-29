import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CaisseService } from '../../core/services/caisse.service';
import { AuthService } from '../../core/services/auth.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import {
  CaisseResponse, MouvementCaisseResponse, SessionCaisseResponse,
  TYPE_MOUVEMENT_LABELS, TYPES_ENTREE_MANUELS, TYPES_SORTIE_MANUELS
} from '../../core/models/caisse.model';
import { modePaiementLabel, modePaiementOptions } from '../../core/models/paiement.model';

/**
 * Tenue de caisse au quotidien : ouverture de vacation avec fond de caisse,
 * suivi des mouvements, saisie manuelle d'une entrée ou d'une sortie, puis
 * comptage de fermeture.
 *
 * L'écart entre le montant théorique et le montant compté est mis en évidence
 * sans être bloquant : constater un manquant fait partie du métier, le masquer
 * non.
 */
@Component({
  selector: 'app-caisse',
  standalone: false,
  templateUrl: './caisse.component.html',
  styleUrl: './caisse.component.scss'
})
export class CaisseComponent implements OnInit {
  caisses: CaisseResponse[] = [];
  caisseActive: CaisseResponse | null = null;
  session: SessionCaisseResponse | null = null;
  mouvements: MouvementCaisseResponse[] = [];

  loading = false;
  loadingMouvements = false;
  saving = false;

  showOuverture = false;
  showFermeture = false;
  showMouvement = false;

  ouvertureForm!: FormGroup;
  fermetureForm!: FormGroup;
  mouvementForm!: FormGroup;

  typeLabels = TYPE_MOUVEMENT_LABELS;
  modeLabels = modePaiementLabel;
  modeOptions = modePaiementOptions;

  constructor(
    private fb: FormBuilder,
    private caisseService: CaisseService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Caisse', 'Vacations, encaissements et décaissements');
    this.initForms();
    this.charger();
  }

  private initForms(): void {
    this.ouvertureForm = this.fb.group({
      fondCaisse: [0, [Validators.required, Validators.min(0)]],
      notes: ['']
    });
    this.fermetureForm = this.fb.group({
      montantCompte: [null, [Validators.required, Validators.min(0)]],
      notes: ['']
    });
    this.mouvementForm = this.fb.group({
      sens: ['SORTIE', Validators.required],
      type: ['DEPENSE', Validators.required],
      montant: [null, [Validators.required, Validators.min(1)]],
      mode: ['ESPECES'],
      libelle: ['', Validators.required],
      reference: [''],
      notes: ['']
    });
  }

  charger(): void {
    this.loading = true;
    this.caisseService.findAll().subscribe({
      next: (data) => {
        this.caisses = data;
        this.loading = false;
        const cible = this.caisseActive
          ? data.find(c => c.id === this.caisseActive!.id)
          : data[0];
        if (cible) this.selectionner(cible);
      },
      error: () => { this.loading = false; }
    });
  }

  selectionner(c: CaisseResponse): void {
    this.caisseActive = c;
    this.session = null;
    this.mouvements = [];
    if (!c.sessionOuverteId) {
      this.chargerDerniersMouvements(c.id);
      return;
    }
    this.caisseService.sessionOuverte(c.id).subscribe({
      next: (s) => {
        this.session = s;
        if (s) this.chargerMouvementsSession(s.id);
      },
      error: () => { this.session = null; }
    });
  }

  private chargerMouvementsSession(sessionId: number): void {
    this.loadingMouvements = true;
    this.caisseService.mouvementsSession(sessionId).subscribe({
      next: (m) => { this.mouvements = m; this.loadingMouvements = false; },
      error: () => { this.loadingMouvements = false; }
    });
  }

  /**
   * Hors vacation, on montre quand même les derniers mouvements de la caisse :
   * l'agent qui arrive doit voir ce qui s'est passé avant lui.
   */
  private chargerDerniersMouvements(caisseId: number): void {
    this.loadingMouvements = true;
    this.caisseService.mouvementsCaisse(caisseId, 0, 30).subscribe({
      next: (page) => { this.mouvements = page.content; this.loadingMouvements = false; },
      error: () => { this.loadingMouvements = false; }
    });
  }

  // ------------------------------------------------------------------
  // Vacation
  // ------------------------------------------------------------------

  ouvrirVacation(): void {
    this.ouvertureForm.reset({ fondCaisse: 0 });
    this.showOuverture = true;
  }

  confirmerOuverture(): void {
    if (!this.caisseActive || this.ouvertureForm.invalid) {
      this.ouvertureForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.caisseService.ouvrir({
      caisseId: this.caisseActive.id,
      fondCaisse: this.ouvertureForm.value.fondCaisse ?? 0,
      notes: this.ouvertureForm.value.notes || undefined
    }).subscribe({
      next: () => {
        this.saving = false;
        this.showOuverture = false;
        this.messageService.add({
          severity: 'success', summary: 'Vacation ouverte',
          detail: `${this.caisseActive!.nom} est en service`
        });
        this.charger();
      },
      error: (err) => this.echec(err, "L'ouverture a échoué")
    });
  }

  fermerVacation(): void {
    if (!this.session) return;
    this.fermetureForm.reset({ montantCompte: this.session.montantTheorique });
    this.showFermeture = true;
  }

  get ecartSimule(): number {
    const compte = this.fermetureForm?.value?.montantCompte;
    if (compte === null || compte === undefined || !this.session) return 0;
    return compte - this.session.montantTheorique;
  }

  confirmerFermeture(): void {
    if (!this.session || this.fermetureForm.invalid) {
      this.fermetureForm.markAllAsTouched();
      return;
    }
    const ecart = this.ecartSimule;
    const suite = () => {
      this.saving = true;
      this.caisseService.fermer(this.session!.id, {
        montantCompte: this.fermetureForm.value.montantCompte,
        notes: this.fermetureForm.value.notes || undefined
      }).subscribe({
        next: (s) => {
          this.saving = false;
          this.showFermeture = false;
          this.messageService.add({
            severity: s.ecart === 0 ? 'success' : 'warn',
            summary: 'Vacation fermée',
            detail: s.ecart === 0
              ? 'Caisse juste, aucun écart'
              : `Écart de ${this.formatMontant(s.ecart)} enregistré`
          });
          this.charger();
        },
        error: (err) => this.echec(err, 'La fermeture a échoué')
      });
    };

    // Un écart passe rarement inaperçu par hasard : on demande confirmation
    // explicite plutôt que de laisser fermer d'un clic distrait.
    if (ecart !== 0) {
      this.confirmationService.confirm({
        header: 'Écart de caisse',
        message: `Le comptage laisse un écart de ${this.formatMontant(ecart)} `
          + `(${ecart > 0 ? 'excédent' : 'manquant'}). Confirmer la fermeture ?`,
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Recompter',
        accept: suite
      });
    } else {
      suite();
    }
  }

  // ------------------------------------------------------------------
  // Mouvement manuel
  // ------------------------------------------------------------------

  nouveauMouvement(sens: 'ENTREE' | 'SORTIE'): void {
    this.mouvementForm.reset({
      sens,
      type: sens === 'ENTREE' ? 'AUTRE_RECETTE' : 'DEPENSE',
      mode: 'ESPECES',
      montant: null,
      libelle: '',
      reference: '',
      notes: ''
    });
    this.showMouvement = true;
  }

  get typesDisponibles() {
    return this.mouvementForm?.value?.sens === 'ENTREE' ? TYPES_ENTREE_MANUELS : TYPES_SORTIE_MANUELS;
  }

  onSensChange(): void {
    const sens = this.mouvementForm.value.sens;
    this.mouvementForm.patchValue({ type: sens === 'ENTREE' ? 'AUTRE_RECETTE' : 'DEPENSE' });
  }

  enregistrerMouvement(): void {
    if (!this.caisseActive || this.mouvementForm.invalid) {
      this.mouvementForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.caisseService.enregistrerMouvement({
      caisseId: this.caisseActive.id,
      ...this.mouvementForm.value
    }).subscribe({
      next: () => {
        this.saving = false;
        this.showMouvement = false;
        this.messageService.add({
          severity: 'success', summary: 'Mouvement enregistré',
          detail: this.mouvementForm.value.libelle
        });
        this.charger();
      },
      error: (err) => this.echec(err, "L'enregistrement a échoué")
    });
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  get totalEntrees(): number {
    return this.mouvements.filter(m => m.sens === 'ENTREE').reduce((t, m) => t + m.montant, 0);
  }

  get totalSorties(): number {
    return this.mouvements.filter(m => m.sens === 'SORTIE').reduce((t, m) => t + m.montant, 0);
  }

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
}
