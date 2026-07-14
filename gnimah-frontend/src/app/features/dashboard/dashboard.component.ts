import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { SejourService } from '../../core/services/sejour.service';
import { ReservationService } from '../../core/services/reservation.service';
import { ChambreService } from '../../core/services/chambre.service';
import { KpiService } from '../../core/services/kpi.service';
import { SejourResponse } from '../../core/models/sejour.model';
import { ReservationResponse } from '../../core/models/reservation.model';
import { ChambreResponse } from '../../core/models/chambre.model';
import { KpiResponse } from '../../core/models/kpi.model';

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  sejoursEnCours: SejourResponse[] = [];
  departsJour: SejourResponse[] = [];
  arriveesDuJour: ReservationResponse[] = [];
  chambres: ChambreResponse[] = [];
  kpi: KpiResponse | null = null;
  loading = true;
  today = new Date();

  constructor(
    public authService: AuthService,
    private sejourService: SejourService,
    private reservationService: ReservationService,
    private chambreService: ChambreService,
    private kpiService: KpiService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.sejourService.findEnCours().subscribe(s => this.sejoursEnCours = s);
    this.sejourService.findDepartsJour().subscribe(d => this.departsJour = d);
    this.reservationService.findArriveesDuJour().subscribe(a => this.arriveesDuJour = a);
    this.chambreService.findAll().subscribe(c => {
      this.chambres = c;
      this.loading = false;
    });
    if (this.authService.hasDirecteurAccess()) {
      this.kpiService.getDashboardToday().subscribe(k => this.kpi = k);
    }
  }

  get chambresLibres(): number {
    return this.chambres.filter(c => c.etat === 'LIBRE').length;
  }

  get chambresOccupees(): number {
    return this.chambres.filter(c => c.etat === 'OCCUPEE').length;
  }

  get chambresANettoyer(): number {
    return this.chambres.filter(c => c.etat === 'A_NETTOYER').length;
  }

  get tauxOccupation(): number {
    if (this.chambres.length === 0) return 0;
    return Math.round((this.chambresOccupees / this.chambres.length) * 100);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(amount || 0);
  }
}
