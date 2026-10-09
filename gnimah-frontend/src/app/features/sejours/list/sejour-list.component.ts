import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SejourService } from '../../../core/services/sejour.service';
import { PaiementService } from '../../../core/services/paiement.service';
import { CaisseService } from '../../../core/services/caisse.service';
import { SejourResponse, TYPE_REMISE_OPTIONS } from '../../../core/models/sejour.model';
import { PaiementResponse, modePaiementOptions } from '../../../core/models/paiement.model';
import { CaisseResponse } from '../../../core/models/caisse.model';
import { MessageService, ConfirmationService, ConfirmEventType } from 'primeng/api';
import { PageHeaderService } from '../../../core/services/page-header.service';
import { formatMontant } from '../../../core/i18n/devise';

@Component({
  selector: 'app-sejour-list',
  standalone: false,
  templateUrl: './sejour-list.component.html',
  styleUrl: './sejour-list.component.scss'
})
export class SejourListComponent implements OnInit {
  sejours: SejourResponse[] = [];
  loading = false;
  totalRecords = 0;
  page = 0;
  rows = 10;
  checkOutLoading = false;
  selectedSejourId: number | null = null;

  caisses: CaisseResponse[] = [];
  modeOptions = modePaiementOptions;
  typeRemiseOptions = TYPE_REMISE_OPTIONS;
  saving = false;

  // ---- Encaissement du solde ----
  showPaiement = false;
  sejourCible: SejourResponse | null = null;
  paiementForm!: FormGroup;
  /** Paiement qui vient d'être enregistré : la fenêtre propose alors son reçu. */
  paiementEnregistre: PaiementResponse | null = null;
  telechargement = false;

  // ---- Révision du séjour (prolongation, tarif, remise) ----
  showRevision = false;
  revisionForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private sejourService: SejourService,
    private paiementService: PaiementService,
    private caisseService: CaisseService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Séjours', 'Arrivées, encaissements, prolongations et départs');
    this.paiementForm = this.fb.group({
      montant: [null, [Validators.required, Validators.min(1)]],
      mode: ['ESPECES', Validators.required],
      referenceTransaction: [''],
      caisseId: [null],
      notes: ['']
    });
    this.revisionForm = this.fb.group({
      dateSortie: [''],
      tarifPersonnalise: [null],
      remiseType: [null],
      remiseValeur: [null],
      remiseMotif: ['']
    });
    this.load();
    this.caisseService.findAll().subscribe({ next: (c) => { this.caisses = c; } });
  }

  load(event?: any): void {
    if (event) {
      this.page = event.first / event.rows;
      this.rows = event.rows;
    }
    this.loading = true;
    this.sejourService.findAll(this.page, this.rows).subscribe({
      next: (data) => {
        this.sejours = data.content;
        this.totalRecords = data.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  // ------------------------------------------------------------------
  // Départ
  // ------------------------------------------------------------------

  confirmCheckOut(sejour: SejourResponse): void {
    if (sejour.resteAPayer > 0) {
      // Le cas normal est d'encaisser le solde avant de libérer la chambre :
      // c'est donc l'action proposée en premier.
      this.confirmationService.confirm({
        message: `${sejour.clientNom} doit encore ${formatMontant(sejour.resteAPayer)}.\n`
          + 'Encaissez le solde avant le départ, ou forcez le départ sans paiement intégral.',
        header: 'Solde impayé',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Encaisser le solde',
        rejectLabel: 'Forcer le départ',
        accept: () => this.ouvrirPaiement(sejour),
        reject: (type: ConfirmEventType) => {
          if (type === ConfirmEventType.REJECT) this.doCheckOut(sejour.id);
        }
      });
      return;
    }
    this.confirmationService.confirm({
      message: `Enregistrer le départ de ${sejour.clientNom} (Chambre ${sejour.chambreNumero}) ?`,
      header: 'Confirmation de départ',
      icon: 'pi pi-sign-out',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => this.doCheckOut(sejour.id)
    });
  }

  doCheckOut(id: number): void {
    this.selectedSejourId = id;
    this.checkOutLoading = true;
    this.sejourService.checkOut(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Départ enregistré', detail: 'La chambre passe « à nettoyer »' });
        this.checkOutLoading = false;
        this.selectedSejourId = null;
        this.load();
      },
      error: (err) => {
        this.checkOutLoading = false;
        this.selectedSejourId = null;
        this.erreur(err, 'Enregistrement du départ impossible');
      }
    });
  }

  // ------------------------------------------------------------------
  // Encaissement du solde
  // ------------------------------------------------------------------

  ouvrirPaiement(sejour: SejourResponse): void {
    this.sejourCible = sejour;
    this.paiementEnregistre = null;
    this.paiementForm.reset({
      montant: sejour.resteAPayer > 0 ? sejour.resteAPayer : null,
      mode: 'ESPECES',
      referenceTransaction: '',
      caisseId: null,
      notes: ''
    });
    this.showPaiement = true;
  }

  get montantSaisiDepasse(): boolean {
    const montant = Number(this.paiementForm?.value?.montant) || 0;
    return !!this.sejourCible && montant > this.sejourCible.resteAPayer;
  }

  enregistrerPaiement(): void {
    if (!this.sejourCible || this.paiementForm.invalid || this.montantSaisiDepasse) {
      this.paiementForm.markAllAsTouched();
      return;
    }
    const v = this.paiementForm.value;
    this.saving = true;
    this.paiementService.enregistrer({
      sejourId: this.sejourCible.id,
      montant: Number(v.montant),
      mode: v.mode,
      referenceTransaction: v.referenceTransaction || undefined,
      notes: v.notes || undefined,
      caisseId: v.caisseId
    }).subscribe({
      next: (p) => {
        this.saving = false;
        this.paiementEnregistre = p;
        this.messageService.add({
          severity: 'success', summary: 'Paiement enregistré',
          detail: `${formatMontant(p.montant)} — reçu ${p.numeroRecu}`
        });
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.erreur(err, "L'encaissement a échoué");
      }
    });
  }

  telechargerRecu(): void {
    const p = this.paiementEnregistre;
    if (!p) return;
    this.telechargement = true;
    this.paiementService.telechargerRecu(p.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recu-${p.numeroRecu}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.telechargement = false;
      },
      error: () => {
        this.telechargement = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Téléchargement du reçu impossible' });
      }
    });
  }

  // ------------------------------------------------------------------
  // Révision : prolongation, tarif négocié, remise
  // ------------------------------------------------------------------

  ouvrirRevision(sejour: SejourResponse): void {
    this.sejourCible = sejour;
    this.revisionForm.reset({
      dateSortie: sejour.dateSortie ? sejour.dateSortie.substring(0, 10) : '',
      tarifPersonnalise: sejour.tarifApplique,
      remiseType: sejour.remiseType,
      remiseValeur: sejour.remiseValeur > 0 ? sejour.remiseValeur : null,
      remiseMotif: sejour.remiseMotif || ''
    });
    this.showRevision = true;
  }

  enregistrerRevision(): void {
    if (!this.sejourCible) return;
    const v = this.revisionForm.value;
    const estSejour = this.sejourCible.typeLocation === 'SEJOUR';
    this.saving = true;
    this.sejourService.appliquerRemise(this.sejourCible.id, {
      dateSortie: estSejour && v.dateSortie ? v.dateSortie : null,
      tarifPersonnalise: Number(v.tarifPersonnalise) > 0 ? Number(v.tarifPersonnalise) : null,
      remiseType: v.remiseType || null,
      remiseValeur: Number(v.remiseValeur) > 0 ? Number(v.remiseValeur) : null,
      remiseMotif: v.remiseMotif || null
    }).subscribe({
      next: (s) => {
        this.saving = false;
        this.showRevision = false;
        this.messageService.add({
          severity: 'success', summary: 'Séjour mis à jour',
          detail: `Nouveau total ${formatMontant(s.montantTotal)} · reste ${formatMontant(s.resteAPayer)}`
        });
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.erreur(err, 'La modification du séjour a échoué');
      }
    });
  }

  // ------------------------------------------------------------------
  // Affichage
  // ------------------------------------------------------------------

  private erreur(err: any, defaut: string): void {
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err?.error?.message || defaut });
  }

  isInvalid(champ: string, form: FormGroup): boolean {
    const c = form.get(champ);
    return !!(c && c.invalid && c.touched);
  }

  statutTone(statut: string): string {
    return { EN_COURS: 'wine', TERMINE: 'green', ANNULE: 'red' }[statut] || 'gray';
  }

  getStatutLabel(statut: string): string {
    return { EN_COURS: 'En cours', TERMINE: 'Terminé', ANNULE: 'Annulé' }[statut] || statut;
  }

  initials(name: string): string {
    return (name || '?').split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }
}
