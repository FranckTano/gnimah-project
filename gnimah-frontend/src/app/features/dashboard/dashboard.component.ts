import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { SejourService } from '../../core/services/sejour.service';
import { ReservationService } from '../../core/services/reservation.service';
import { ChambreService } from '../../core/services/chambre.service';
import { KpiService } from '../../core/services/kpi.service';
import { UtilisateurService } from '../../core/services/utilisateur.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import { SejourResponse } from '../../core/models/sejour.model';
import { ReservationResponse } from '../../core/models/reservation.model';
import { ChambreResponse } from '../../core/models/chambre.model';
import { KpiResponse } from '../../core/models/kpi.model';

interface ChartBar {
  x: number; w: number; y: number; h: number; day: string; dayX: number; label: string; labelX: number; labelY: number;
}

interface Todo {
  icon: string; label: string; tag: string; bg: string; cls: string;
}

interface IndicatorRow {
  l: string; s: string; v: string; w: number;
}

interface AgentRow {
  name: string; count: string; ca: string; initials: string; tone: string;
}

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
  kpiMonthly: KpiResponse | null = null;
  reservationsAVenir = 0;
  agentNames: Record<number, string> = {};
  loading = true;

  constructor(
    public authService: AuthService,
    private sejourService: SejourService,
    private reservationService: ReservationService,
    private chambreService: ChambreService,
    private kpiService: KpiService,
    private utilisateurService: UtilisateurService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    const dir = this.authService.hasDirecteurAccess();
    this.pageHeaderService.set(
      dir ? 'Tableau de bord' : `Bonjour, ${this.authService.currentUser?.prenom || ''} 👋`,
      dir ? "Vue d'ensemble de l'activité" : 'Voici votre journée à la réception'
    );
    this.loadDashboard();
  }

  get isDirecteur(): boolean {
    return this.authService.hasDirecteurAccess();
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
    this.reservationService.findAll(0, 50).subscribe(page => {
      const today = new Date().toISOString().split('T')[0];
      this.reservationsAVenir = page.content.filter(r =>
        (r.statut === 'EN_ATTENTE' || r.statut === 'CONFIRMEE') && r.dateArrivee >= today
      ).length;
    });
    if (this.isDirecteur) {
      this.kpiService.getDashboardToday().subscribe(k => this.kpi = k);
      const debutMois = new Date();
      debutMois.setDate(1);
      this.kpiService.getDashboard(debutMois.toISOString().split('T')[0]).subscribe(k => this.kpiMonthly = k);
      this.utilisateurService.findAll().subscribe(users => {
        users.forEach(u => this.agentNames[u.id] = `${u.prenom} ${u.nom}`.trim());
      });
    }
  }

  get totalChambres(): number {
    return this.chambres.length;
  }

  get chambresLibres(): number {
    return this.chambres.filter(c => c.etat === 'LIBRE').length;
  }

  get chambresOccupees(): number {
    return this.chambres.filter(c => c.etat === 'OCCUPEE').length;
  }

  get chambresEnMaintenance(): number {
    return this.chambres.filter(c => c.etat === 'EN_MAINTENANCE').length;
  }

  get chambresANettoyer(): ChambreResponse[] {
    return this.chambres.filter(c => c.etat === 'A_NETTOYER');
  }

  get tauxOccupation(): number {
    return this.kpi?.tauxOccupation ?? 0;
  }

  get occupationDashArray(): string {
    const circumference = 2 * Math.PI * 52;
    const filled = (this.tauxOccupation / 100) * circumference;
    return `${filled.toFixed(1)} ${circumference.toFixed(1)}`;
  }

  get chartBars(): ChartBar[] {
    const data = this.kpi?.caParJour || [];
    if (data.length === 0) return [];
    const max = Math.max(...data.map(d => d.ca), 1);
    const chartH = 150;
    return data.map((d, i) => {
      const h = Math.max(4, Math.round((d.ca / max) * chartH));
      const x = 24 + i * (536 / Math.max(data.length, 1));
      const w = Math.min(34, 480 / Math.max(data.length, 1));
      const y = 185 - h;
      const dayLabel = this.formatWeekday(d.date);
      return { x, w, y, h, day: dayLabel, dayX: x + w / 2, label: this.formatShortAmount(d.ca), labelX: x + w / 2, labelY: y - 7 };
    });
  }

  get reservationChartBars(): ChartBar[] {
    const data = this.kpi?.reservationsParJour || [];
    if (data.length === 0) return [];
    const max = Math.max(...data.map(d => d.count), 1);
    const chartH = 150;
    return data.map((d, i) => {
      const h = Math.max(4, Math.round((d.count / max) * chartH));
      const x = 24 + i * (536 / Math.max(data.length, 1));
      const w = Math.min(34, 480 / Math.max(data.length, 1));
      const y = 185 - h;
      const dayLabel = this.formatWeekday(d.date);
      return { x, w, y, h, day: dayLabel, dayX: x + w / 2, label: `${d.count}`, labelX: x + w / 2, labelY: y - 7 };
    });
  }

  private formatWeekday(dateStr: string): string {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleDateString('fr-FR', { weekday: 'short' }).replace('.', '');
  }

  private formatShortAmount(v: number): string {
    if (v >= 1000) return Math.round(v / 1000) + 'k';
    return '' + Math.round(v);
  }

  get indicators(): IndicatorRow[] {
    if (!this.kpi) return [];
    return [
      { l: 'Tarif moyen/ch.', s: 'Prix moyen facturé par chambre occupée', v: this.formatCurrency(this.kpi.adr), w: this.pct(this.kpi.adr, 40000) },
      { l: 'Revenu/chambre', s: 'Revenu moyen par chambre disponible', v: this.formatCurrency(this.kpi.revPar), w: this.pct(this.kpi.revPar, 30000) },
      { l: 'Rev. total/ch.', s: 'Revenu total (tous services) par chambre dispo.', v: this.formatCurrency(this.kpi.tRevPar), w: this.pct(this.kpi.tRevPar, 30000) },
      { l: 'Durée moy. séjour', s: 'Nombre moyen de nuits par séjour', v: `${(this.kpi.alos || 0).toFixed(1)} nuit`, w: this.pct(this.kpi.alos, 5) }
    ];
  }

  private pct(v: number, max: number): number {
    return Math.max(4, Math.min(100, Math.round(((v || 0) / max) * 100)));
  }

  private readonly tones = ['deep', 'wine', 'gold', 'slate', 'green'];

  get agentPerformance(): AgentRow[] {
    const rows = this.kpi?.performanceAgents || [];
    return rows.map((a, i) => {
      const name = this.agentNames[a.agentId] || `Agent #${a.agentId}`;
      const initials = name.split(' ').map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
      return {
        name,
        count: `${a.nbSejours} enregistrement${a.nbSejours > 1 ? 's' : ''}`,
        ca: this.formatCurrency(a.ca),
        initials,
        tone: this.tones[i % this.tones.length]
      };
    });
  }

  get agentTodos(): Todo[] {
    const todos: Todo[] = [];
    this.chambresANettoyer.slice(0, 3).forEach(c => {
      todos.push({ icon: 'ti-brush', label: `Chambre ${c.numero} à nettoyer`, tag: 'Entretien', bg: '#fdf4e3', cls: 'c-gold' });
    });
    this.sejoursEnCours.filter(s => s.resteAPayer > 0).slice(0, 3).forEach(s => {
      todos.push({ icon: 'ti-cash', label: `Reste à payer — ${s.clientNom} (${this.formatCurrency(s.resteAPayer)})`, tag: 'Caisse', bg: '#fbeef1', cls: 'c-wine' });
    });
    this.departsJour.slice(0, 3).forEach(s => {
      todos.push({ icon: 'ti-door-exit', label: `Départ chambre ${s.chambreNumero} prévu`, tag: 'Départ', bg: '#eef7f2', cls: 'c-green' });
    });
    return todos;
  }

  formatCurrency(amount: number | undefined): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(amount || 0);
  }
}
