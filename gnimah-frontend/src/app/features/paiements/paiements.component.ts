import { Component, OnInit } from '@angular/core';
import { PaiementService } from '../../core/services/paiement.service';
import { PaiementResponse, modePaiementLabel } from '../../core/models/paiement.model';
import { SejourService } from '../../core/services/sejour.service';
import { SejourResponse } from '../../core/models/sejour.model';
import { MessageService } from 'primeng/api';
import { PageHeaderService } from '../../core/services/page-header.service';

@Component({
  selector: 'app-paiements',
  standalone: false,
  templateUrl: './paiements.component.html',
  styleUrl: './paiements.component.scss'
})
export class PaiementsComponent implements OnInit {
  paiements: PaiementResponse[] = [];
  loading = false;
  totalRecords = 0;
  page = 0;
  rows = 20;
  modeLabels = modePaiementLabel;
  downloadingId: number | null = null;

  selectedPaiement: PaiementResponse | null = null;
  selectedSejour: SejourResponse | null = null;
  loadingDetail = false;

  constructor(
    private paiementService: PaiementService,
    private sejourService: SejourService,
    private messageService: MessageService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Reçus & factures', 'Historique des paiements et documents');
    this.load();
  }

  load(event?: any): void {
    if (event) {
      this.page = event.first / event.rows;
      this.rows = event.rows;
    }
    this.loading = true;
    this.paiementService.findAll(this.page, this.rows).subscribe({
      next: (data) => {
        this.paiements = data.content;
        this.totalRecords = data.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  voirRecu(p: PaiementResponse): void {
    this.selectedPaiement = p;
    this.loadingDetail = true;
    this.sejourService.getById(p.sejourId).subscribe({
      next: (s) => { this.selectedSejour = s; this.loadingDetail = false; },
      error: () => { this.loadingDetail = false; this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Détail du séjour indisponible' }); }
    });
  }

  retourListe(): void {
    this.selectedPaiement = null;
    this.selectedSejour = null;
  }

  imprimer(): void {
    window.print();
  }

  telechargerRecu(p: PaiementResponse): void {
    this.downloadingId = p.id;
    this.paiementService.telechargerRecu(p.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recu-${p.numeroRecu}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.downloadingId = null;
      },
      error: () => {
        this.downloadingId = null;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Téléchargement impossible' });
      }
    });
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
  }
}
