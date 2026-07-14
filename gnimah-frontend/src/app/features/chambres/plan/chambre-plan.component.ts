import { Component, OnInit } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ChambreService } from '../../../core/services/chambre.service';
import {
  ChambreResponse,
  EtatChambre,
  ETAT_CHAMBRE_LABELS,
  ETAT_CHAMBRE_COLORS
} from '../../../core/models/chambre.model';

@Component({
  selector: 'app-chambre-plan',
  standalone: false,
  templateUrl: './chambre-plan.component.html',
  styleUrl: './chambre-plan.component.scss'
})
export class ChambrePlanComponent implements OnInit {
  chambres: ChambreResponse[] = [];
  loading = false;

  // Detail dialog
  selectedChambre: ChambreResponse | null = null;
  detailVisible = false;
  newEtat: EtatChambre = 'LIBRE';
  updatingEtat = false;

  etatOptions: { label: string; value: EtatChambre }[] = [
    { label: 'Libre', value: 'LIBRE' },
    { label: 'Occupée', value: 'OCCUPEE' },
    { label: 'À nettoyer', value: 'A_NETTOYER' },
    { label: 'En maintenance', value: 'EN_MAINTENANCE' },
    { label: 'Hors service', value: 'HORS_SERVICE' }
  ];

  etatLabels = ETAT_CHAMBRE_LABELS;
  etatColors = ETAT_CHAMBRE_COLORS;

  constructor(
    private chambreService: ChambreService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadChambres();
  }

  loadChambres(): void {
    this.loading = true;
    this.chambreService.findAll().subscribe({
      next: (data) => {
        this.chambres = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les chambres' });
      }
    });
  }

  openDetail(chambre: ChambreResponse): void {
    this.selectedChambre = chambre;
    this.newEtat = chambre.etat;
    this.detailVisible = true;
  }

  saveEtat(): void {
    if (!this.selectedChambre) return;
    this.updatingEtat = true;
    this.chambreService.updateEtat(this.selectedChambre.id, this.newEtat).subscribe({
      next: (updated) => {
        const idx = this.chambres.findIndex(c => c.id === updated.id);
        if (idx !== -1) this.chambres[idx] = updated;
        this.chambres = [...this.chambres];
        this.detailVisible = false;
        this.updatingEtat = false;
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'État mis à jour' });
      },
      error: () => {
        this.updatingEtat = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Mise à jour impossible' });
      }
    });
  }

  get totalChambres(): number { return this.chambres.length; }
  get nbLibres(): number { return this.chambres.filter(c => c.etat === 'LIBRE').length; }
  get nbOccupees(): number { return this.chambres.filter(c => c.etat === 'OCCUPEE').length; }
  get nbANettoyer(): number { return this.chambres.filter(c => c.etat === 'A_NETTOYER').length; }
  get nbEnMaintenance(): number { return this.chambres.filter(c => c.etat === 'EN_MAINTENANCE').length; }
  get nbHorsService(): number { return this.chambres.filter(c => c.etat === 'HORS_SERVICE').length; }

  getChambreClass(etat: EtatChambre): string {
    return 'chambre-card chambre-' + etat.toLowerCase().replace('_', '-');
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-CI', {
      style: 'currency', currency: 'XOF', maximumFractionDigits: 0
    }).format(amount || 0);
  }
}
