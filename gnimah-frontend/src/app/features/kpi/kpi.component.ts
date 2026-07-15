import { Component, OnInit } from '@angular/core';
import { KpiService } from '../../core/services/kpi.service';
import { UtilisateurService } from '../../core/services/utilisateur.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import { KpiResponse } from '../../core/models/kpi.model';

@Component({
  selector: 'app-kpi',
  standalone: false,
  templateUrl: './kpi.component.html',
  styleUrl: './kpi.component.scss'
})
export class KpiComponent implements OnInit {
  kpi: KpiResponse | null = null;
  loading = false;
  dateDebut = '';
  dateFin = '';
  agentNames: Record<number, string> = {};

  chartCA: any = null;
  chartOccupation: any = null;
  chartOptions: any;

  constructor(
    private kpiService: KpiService,
    private utilisateurService: UtilisateurService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Statistiques & KPI', 'Analyse détaillée de la performance sur une période');
    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { beginAtZero: true, grid: { color: '#f2e8e4' }, ticks: { color: '#a99a9d' } },
        x: { grid: { display: false }, ticks: { color: '#a99a9d' } }
      }
    };
    this.utilisateurService.findAll().subscribe(users => {
      users.forEach(u => this.agentNames[u.id] = `${u.prenom} ${u.nom}`.trim());
    });
    this.load();
  }

  agentName(id: number): string {
    return this.agentNames[id] || `Agent #${id}`;
  }

  agentInitials(id: number): string {
    const name = this.agentName(id);
    return name.split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  load(): void {
    this.loading = true;
    const debut = this.dateDebut || undefined;
    const fin = this.dateFin || undefined;
    this.kpiService.getDashboard(debut, fin).subscribe({
      next: (data) => {
        this.kpi = data;
        this.buildCharts(data);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  buildCharts(data: KpiResponse): void {
    if (data.caParJour && data.caParJour.length > 0) {
      this.chartCA = {
        labels: data.caParJour.map(d => d.date),
        datasets: [{
          label: 'CA (XOF)',
          data: data.caParJour.map(d => d.ca),
          backgroundColor: 'rgba(140,47,77,0.12)',
          borderColor: '#8c2f4d',
          pointBackgroundColor: '#8c2f4d',
          borderWidth: 2,
          fill: true,
          tension: 0.35
        }]
      };
    }

    this.chartOccupation = {
      labels: ['Libres', 'Occupées', 'À nettoyer', 'Maintenance'],
      datasets: [{
        data: [
          data.chambresLibres,
          data.chambresOccupees,
          data.chambresANettoyer,
          data.chambresEnMaintenance
        ],
        backgroundColor: ['#2f9e6f', '#b23a55', '#c9922f', '#5f7191'],
        borderWidth: 0
      }]
    };
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
  }

  formatPct(v: number): string {
    return `${(v || 0).toFixed(1)}%`;
  }

  resetFiltres(): void {
    this.dateDebut = '';
    this.dateFin = '';
    this.load();
  }
}
