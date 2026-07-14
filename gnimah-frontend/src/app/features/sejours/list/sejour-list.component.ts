import { Component, OnInit } from '@angular/core';
import { SejourService } from '../../../core/services/sejour.service';
import { SejourResponse } from '../../../core/models/sejour.model';
import { MessageService, ConfirmationService } from 'primeng/api';

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

  constructor(
    private sejourService: SejourService,
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
    this.sejourService.findAll(this.page, this.rows).subscribe({
      next: (data) => {
        this.sejours = data.content;
        this.totalRecords = data.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  confirmCheckOut(sejour: SejourResponse): void {
    this.confirmationService.confirm({
      message: `Effectuer le check-out de ${sejour.clientNom} (Chambre ${sejour.chambreNumero}) ?`,
      header: 'Confirmation Check-Out',
      icon: 'pi pi-sign-out',
      accept: () => this.doCheckOut(sejour.id)
    });
  }

  doCheckOut(id: number): void {
    this.selectedSejourId = id;
    this.checkOutLoading = true;
    this.sejourService.checkOut(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Check-Out', detail: 'Check-out effectué avec succès' });
        this.checkOutLoading = false;
        this.selectedSejourId = null;
        this.load();
      },
      error: () => {
        this.checkOutLoading = false;
        this.selectedSejourId = null;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du check-out' });
      }
    });
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
  }

  getStatutSeverity(statut: string): string {
    return { EN_COURS: 'info', TERMINE: 'success', ANNULE: 'danger' }[statut] || 'secondary';
  }

  getStatutLabel(statut: string): string {
    return { EN_COURS: 'En cours', TERMINE: 'Terminé', ANNULE: 'Annulé' }[statut] || statut;
  }
}
