import { Component, OnInit } from '@angular/core';
import { ReservationService } from '../../../core/services/reservation.service';
import { ReservationResponse, STATUT_RESERVATION_LABELS } from '../../../core/models/reservation.model';
import { MessageService, ConfirmationService } from 'primeng/api';

@Component({
  selector: 'app-reservation-list',
  standalone: false,
  templateUrl: './reservation-list.component.html',
  styleUrl: './reservation-list.component.scss'
})
export class ReservationListComponent implements OnInit {
  reservations: ReservationResponse[] = [];
  loading = false;
  totalRecords = 0;
  page = 0;
  rows = 10;
  statutLabels: Record<string, string> = STATUT_RESERVATION_LABELS;

  constructor(
    private reservationService: ReservationService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void { this.load(); }

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

  confirmer(r: ReservationResponse): void {
    this.reservationService.confirmer(r.id).subscribe({
      next: () => { this.messageService.add({ severity: 'success', summary: 'Confirmée', detail: 'Réservation confirmée' }); this.load(); },
      error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Confirmation impossible' })
    });
  }

  annuler(r: ReservationResponse): void {
    this.confirmationService.confirm({
      message: `Annuler la réservation de ${r.clientNom} ?`,
      header: 'Annulation',
      icon: 'pi pi-times',
      accept: () => {
        this.reservationService.annuler(r.id).subscribe({
          next: () => { this.messageService.add({ severity: 'warn', summary: 'Annulée', detail: 'Réservation annulée' }); this.load(); },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Annulation impossible' })
        });
      }
    });
  }

  getStatutSeverity(s: string): string {
    return { EN_ATTENTE: 'warn', CONFIRMEE: 'success', ARRIVEE: 'info', ANNULEE: 'danger', NO_SHOW: 'secondary' }[s] || 'secondary';
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
  }
}
