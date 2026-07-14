import { Component, OnInit } from '@angular/core';
import { KpiService } from '../../core/services/kpi.service';
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
  dateDebut: Date | null = null;
  dateFin: Date | null = null;

  chartCA: any = null;
  chartOccupation: any = null;
  chartOptions: any;

  constructor(private kpiService: KpiService) {}

  ngOnInit(): void {
    this.chartOptions = {
      responsive: true,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } }
    };
    this.load();
  }

  load(): void {
    this.loading = true;
    const debut = this.dateDebut ? this.dateDebut.toISOString().split('T')[0] : undefined;
    const fin = this.dateFin ? this.dateFin.toISOString().split('T')[0] : undefined;
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
          backgroundColor: 'rgba(59,130,246,0.2)',
          borderColor: '#3b82f6',
          borderWidth: 2,
          fill: true,
          tension: 0.4
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
        backgroundColor: ['#22c55e', '#ef4444', '#f59e0b', '#8b5cf6']
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
    this.dateDebut = null;
    this.dateFin = null;
    this.load();
  }
}
