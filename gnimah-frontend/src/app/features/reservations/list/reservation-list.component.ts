import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ReservationService } from '../../../core/services/reservation.service';
import { CaisseService } from '../../../core/services/caisse.service';
import {
  PolitiqueAnnulation, PREAVIS_ANNULATION_HEURES,
  ReservationResponse, STATUT_RESERVATION_LABELS
} from '../../../core/models/reservation.model';
import { CaisseResponse } from '../../../core/models/caisse.model';
import { modePaiementLabel, modePaiementOptions } from '../../../core/models/paiement.model';
import { MessageService } from 'primeng/api';
import { Router } from '@angular/router';
import { PageHeaderService } from '../../../core/services/page-header.service';
import { formatMontant } from '../../../core/i18n/devise';

/**
 * Planning des réservations.
 *
 * Deux règles de maison pilotent cet écran : une réservation ne se confirme que
 * par un acompte encaissé, et l'annulation doit être annoncée au moins 48 h
 * avant l'arrivée sous peine de perdre cet acompte. L'agent voit donc, avant de
 * valider quoi que ce soit, ce que la décision coûte au client.
 */
@Component({
  selector: 'app-reservation-list',
  standalone: false,
  templateUrl: './reservation-list.component.html',
  styleUrl: './reservation-list.component.scss'
})
export class ReservationListComponent implements OnInit {
  reservations: ReservationResponse[] = [];
  caisses: CaisseResponse[] = [];
  loading = false;
  saving = false;
  totalRecords = 0;
  page = 0;
  rows = 20;
  statutLabels: Record<string, string> = STATUT_RESERVATION_LABELS;
  preavisHeures = PREAVIS_ANNULATION_HEURES;

  detailVisible = false;
  selectedReservation: ReservationResponse | null = null;

  // ---- Acompte ----
  showAcompte = false;
  reservationCible: ReservationResponse | null = null;
  acompteForm!: FormGroup;

  // ---- Annulation ----
  showAnnulation = false;
  politique: PolitiqueAnnulation | null = null;
  annulationForm!: FormGroup;
  chargementPolitique = false;

  modeOptions = modePaiementOptions;
  modeLabels = modePaiementLabel;

  constructor(
    private fb: FormBuilder,
    private reservationService: ReservationService,
    private caisseService: CaisseService,
    private messageService: MessageService,
    private pageHeaderService: PageHeaderService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Réservations', 'Acomptes, confirmations et annulations');
    this.initForms();
    this.load();
    this.caisseService.findAll().subscribe({ next: (d) => { this.caisses = d; } });
  }

  private initForms(): void {
    this.acompteForm = this.fb.group({
      montant: [null, [Validators.required, Validators.min(1)]],
      mode: ['ESPECES', Validators.required],
      referenceTransaction: [''],
      caisseId: [null],
      notes: ['']
    });

    this.annulationForm = this.fb.group({
      motif: [''],
      rembourser: [false],
      caisseId: [null],
      mode: ['ESPECES']
    });
  }

  get statCount() {
    const r = this.reservations;
    return {
      confirmees: r.filter(x => x.statut === 'CONFIRMEE').length,
      enAttente: r.filter(x => x.statut === 'EN_ATTENTE').length,
      arrivees: r.filter(x => x.statut === 'ARRIVEE').length,
      annulees: r.filter(x => x.statut === 'ANNULEE').length,
      noShow: r.filter(x => x.statut === 'NO_SHOW').length
    };
  }

  /** Acomptes encore attendus sur les réservations non confirmées. */
  get acomptesEnAttente(): number {
    return this.reservations
      .filter(r => r.statut === 'EN_ATTENTE')
      .reduce((t, r) => t + (r.resteAcompte || 0), 0);
  }

  statutTone(s: string): string {
    return { EN_ATTENTE: 'amber', CONFIRMEE: 'green', ARRIVEE: 'wine', ANNULEE: 'gray', NO_SHOW: 'red' }[s] || 'gray';
  }

  initials(name: string): string {
    return (name || '?').split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  load(event?: any): void {
    if (event) {
      this.page = event.first / event.rows;
      this.rows = event.rows;
    }
    this.loading = true;
    this.reservationService.findAll(this.page, this.rows).subscribe({
      next: (data) => {
        this.reservations = data.content;
        this.totalRecords = data.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  // ------------------------------------------------------------------
  // Acompte
  // ------------------------------------------------------------------

  /** Une réservation ne se confirme que par l'argent : le bouton mène à l'acompte. */
  ouvrirAcompte(r: ReservationResponse): void {
    this.reservationCible = r;
    this.acompteForm.reset({
      montant: r.resteAcompte > 0 ? r.resteAcompte : null,
      mode: 'ESPECES',
      caisseId: null
    });
    this.showAcompte = true;
  }

  enregistrerAcompte(): void {
    if (!this.reservationCible || this.acompteForm.invalid) {
      this.acompteForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.reservationService.enregistrerAcompte(this.reservationCible.id, this.acompteForm.value).subscribe({
      next: (r) => {
        this.saving = false;
        this.showAcompte = false;
        this.messageService.add({
          severity: 'success',
          summary: r.statut === 'CONFIRMEE' ? 'Réservation confirmée' : 'Acompte encaissé',
          detail: r.statut === 'CONFIRMEE'
            ? `${r.clientNom} — acompte de ${formatMontant(r.acompte)} reçu`
            : `Il reste ${formatMontant(r.resteAcompte)} pour confirmer`
        });
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || "L'encaissement de l'acompte a échoué"
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Annulation
  // ------------------------------------------------------------------

  /**
   * Interroge le backend sur les conséquences de l'annulation avant de la
   * proposer : l'agent doit pouvoir annoncer la pénalité au client au téléphone,
   * pas la découvrir après coup.
   */
  ouvrirAnnulation(r: ReservationResponse): void {
    this.reservationCible = r;
    this.politique = null;
    this.chargementPolitique = true;
    this.annulationForm.reset({ motif: '', rembourser: false, mode: 'ESPECES', caisseId: null });
    this.showAnnulation = true;

    this.reservationService.politiqueAnnulation(r.id).subscribe({
      next: (p) => {
        this.politique = p;
        this.chargementPolitique = false;
        // Un remboursement dû est proposé par défaut : c'est le cas normal
        // d'une annulation prévenue dans les délais.
        this.annulationForm.patchValue({ rembourser: p.remboursement > 0 });
      },
      error: () => { this.chargementPolitique = false; }
    });
  }

  confirmerAnnulation(): void {
    if (!this.reservationCible) return;
    this.saving = true;
    this.reservationService.annuler(this.reservationCible.id, this.annulationForm.value).subscribe({
      next: (r) => {
        this.saving = false;
        this.showAnnulation = false;
        this.messageService.add({
          severity: r.penaliteAnnulation > 0 ? 'warn' : 'success',
          summary: 'Réservation annulée',
          detail: r.penaliteAnnulation > 0
            ? `Pénalité de ${formatMontant(r.penaliteAnnulation)} retenue`
            : (r.montantRembourse > 0
                ? `${formatMontant(r.montantRembourse)} remboursés au client`
                : 'Annulation sans frais')
        });
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || "L'annulation a échoué"
        });
      }
    });
  }

  // ------------------------------------------------------------------
  // Divers
  // ------------------------------------------------------------------

  voirDetails(r: ReservationResponse): void {
    this.selectedReservation = r;
    this.detailVisible = true;
  }

  /**
   * L'arrivée d'un client se traduit par un séjour : l'écran d'enregistrement
   * s'ouvre pré-rempli depuis la réservation, et l'acompte déjà versé vient en
   * déduction de la note. La réservation passe « arrivée » à la validation.
   */
  marquerArrivee(r: ReservationResponse): void {
    this.router.navigate(['/sejours/check-in'], { queryParams: { reservation: r.id } });
  }

  /** Heures restantes avant l'arrivée — sert au bandeau d'alerte de la liste. */
  heuresAvant(r: ReservationResponse): number {
    return Math.round((new Date(r.dateArrivee).getTime() - Date.now()) / 3_600_000);
  }

  estUrgente(r: ReservationResponse): boolean {
    const h = this.heuresAvant(r);
    return r.statut === 'EN_ATTENTE' && h > 0 && h <= this.preavisHeures;
  }

  isInvalid(champ: string, form: FormGroup): boolean {
    const c = form.get(champ);
    return !!(c && c.invalid && c.touched);
  }
}
