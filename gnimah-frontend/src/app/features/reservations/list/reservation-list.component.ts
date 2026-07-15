import { Component, OnInit } from '@angular/core';
import { ReservationService } from '../../../core/services/reservation.service';
import { ReservationResponse, STATUT_RESERVATION_LABELS } from '../../../core/models/reservation.model';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PageHeaderService } from '../../../core/services/page-header.service';

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
  rows = 20;
  statutLabels: Record<string, string> = STATUT_RESERVATION_LABELS;

  constructor(
    private reservationService: ReservationService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Réservations', 'Planning des arrivées et statuts');
    this.load();
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
