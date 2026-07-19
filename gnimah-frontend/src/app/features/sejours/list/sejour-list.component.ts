import { Component, OnInit } from '@angular/core';
import { SejourService } from '../../../core/services/sejour.service';
import { SejourResponse } from '../../../core/models/sejour.model';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PageHeaderService } from '../../../core/services/page-header.service';

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
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Séjours en cours', 'Liste de tous les séjours enregistrés');
    this.load();
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

  confirmCheckOut(sejour: SejourResponse): void {
    if (sejour.resteAPayer > 0) {
      this.confirmationService.confirm({
        message: `Attention — ce client a un solde impayé de ${this.formatMontant(sejour.resteAPayer)}.\nVoulez-vous quand même enregistrer son départ sans paiement intégral ?`,
        header: 'Solde impayé',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Forcer le départ',
        rejectLabel: 'Annuler',
        accept: () => this.doCheckOut(sejour.id)
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
        this.messageService.add({ severity: 'success', summary: 'Départ enregistré', detail: 'La chambre a été libérée' });
        this.checkOutLoading = false;
        this.selectedSejourId = null;
        this.load();
      },
      error: () => {
        this.checkOutLoading = false;
        this.selectedSejourId = null;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Enregistrement du départ impossible' });
      }
    });
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
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
