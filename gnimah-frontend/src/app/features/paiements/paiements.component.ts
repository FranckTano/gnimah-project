import { Component, OnInit } from '@angular/core';
import { PaiementService } from '../../core/services/paiement.service';
import { PaiementResponse, modePaiementLabel } from '../../core/models/paiement.model';
import { MessageService } from 'primeng/api';

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

  constructor(
    private paiementService: PaiementService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void { this.load(); }

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
