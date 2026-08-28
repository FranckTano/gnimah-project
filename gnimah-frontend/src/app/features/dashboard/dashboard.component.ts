import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { SejourService } from '../../core/services/sejour.service';
import { ReservationService } from '../../core/services/reservation.service';
import { ChambreService } from '../../core/services/chambre.service';
import { KpiService } from '../../core/services/kpi.service';
import { UtilisateurService } from '../../core/services/utilisateur.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import { MediaService } from '../../core/services/media.service';
import { SejourResponse } from '../../core/models/sejour.model';
import { ReservationResponse } from '../../core/models/reservation.model';
import { ChambreResponse } from '../../core/models/chambre.model';
import { KpiResponse } from '../../core/models/kpi.model';
import { Periode, libellePeriode, nbJours } from '../../shared/periode';
import { SERIES, ETAT_SERIES, montantComplet } from '../../shared/chart-theme';
import { PLUGIN_CURSEUR, courbeMontantsParJour, colonnesParJour } from '../../shared/chart-builders';

interface Todo {
  icon: string; label: string; tag: string; bg: string; cls: string;
}

interface SegmentEtat {
  cle: string; label: string; valeur: number; couleur: string;
}

interface AgentRow {
  name: string; count: string; ca: string; initials: string; part: number; couleur: string;
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
  reservationsAVenir = 0;
  agentNames: Record<number, string> = {};

  /** Vrai tant que l'écran n'a jamais rien affiché : ensuite on garde le rendu. */
  premierChargement = true;
  loading = true;

  periode: Periode = { debut: '', fin: '', preset: 'jour' };
  photoAccueil = '';
  today = new Date();

  // ---- Graphiques ----------------------------------------------------------
  pluginCurseur = PLUGIN_CURSEUR;
  chartCA: any = null;
  optionsCA: any = null;
  chartResa: any = null;
  optionsResa: any = null;
  private nbPointsCA = 0;
  private nbPointsResa = 0;

  // ---- Blocs dérivés -------------------------------------------------------
  caTotal = 0;
  caMoyenParJour = 0;
  meilleureJournee: { date: string; ca: number } | null = null;
  partEncaissee = 0;
  segmentsEtat: SegmentEtat[] = [];
  agents: AgentRow[] = [];

  constructor(
    public authService: AuthService,
    private sejourService: SejourService,
    private reservationService: ReservationService,
    private chambreService: ChambreService,
    private kpiService: KpiService,
    private utilisateurService: UtilisateurService,
    private pageHeaderService: PageHeaderService,
    private media: MediaService
  ) {}

  ngOnInit(): void {
    const dir = this.authService.hasDirecteurAccess();
    this.pageHeaderService.set(
      dir ? 'Tableau de bord' : `Bonjour, ${this.authService.currentUser?.prenom || ''} 👋`,
      dir ? "Vue d'ensemble de l'activité" : 'Voici votre journée à la réception'
    );
    const bandeaux = this.media.bandeaux;
    this.photoAccueil = bandeaux[this.today.getDate() % bandeaux.length].src;

    if (dir) {
      this.utilisateurService.findAll().subscribe(users => {
        users.forEach(u => this.agentNames[u.id] = `${u.prenom} ${u.nom}`.trim());
        if (this.kpi) this.construireAgents(this.kpi);
      });
    }
    this.chargerTempsReel();
  }

  get isDirecteur(): boolean {
    return this.authService.hasDirecteurAccess();
  }

  /** Salutation calée sur l'heure locale d'Abidjan (le poste de réception y est). */
  get salutation(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Bonjour';
    if (h < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  get prenomUtilisateur(): string {
    return this.authService.currentUser?.prenom || '';
  }

  get libellePeriode(): string {
    return libellePeriode(this.periode);
  }

  get nbJoursPeriode(): number {
    return nbJours(this.periode);
  }

  // ==========================================================================
  //  Chargement
  // ==========================================================================

  /**
   * Blocs « en temps réel » : ils décrivent l'instant présent (parc, arrivées et
   * départs du jour) et ne dépendent donc pas de la période choisie.
   */
  private chargerTempsReel(): void {
    this.sejourService.findEnCours().subscribe(s => this.sejoursEnCours = s);
    this.sejourService.findDepartsJour().subscribe(d => this.departsJour = d);
    this.reservationService.findArriveesDuJour().subscribe(a => this.arriveesDuJour = a);
    this.chambreService.findAll().subscribe(c => {
      this.chambres = c;
      this.construireEtatParc();
      if (!this.isDirecteur) { this.loading = false; this.premierChargement = false; }
    });
    this.reservationService.findAll(0, 50).subscribe(page => {
      const auj = new Date().toISOString().split('T')[0];
      this.reservationsAVenir = page.content.filter(r =>
        (r.statut === 'EN_ATTENTE' || r.statut === 'CONFIRMEE') && r.dateArrivee >= auj
      ).length;
    });
  }

  /** Appelé par la barre de filtres, à l'initialisation puis à chaque changement. */
  onPeriode(p: Periode): void {
    this.periode = p;
    if (!this.isDirecteur) return;
    this.loading = true;
    this.kpiService.getDashboard(p.debut || undefined, p.fin || undefined).subscribe({
      next: (data) => {
        this.kpi = data;
        this.construireVues(data);
        this.loading = false;
        this.premierChargement = false;
      },
      error: () => { this.loading = false; this.premierChargement = false; }
    });
  }

  private construireVues(data: KpiResponse): void {
    const ca = Number(data.chiffreAffaires) || 0;
    this.partEncaissee = ca > 0 ? Math.min(100, (Number(data.encaissements) || 0) / ca * 100) : 0;

    const points = (data.caParJour || []).map(d => ({ date: d.date, valeur: Number(d.ca) || 0 }));
    this.nbPointsCA = points.length;
    this.caTotal = points.reduce((s, p) => s + p.valeur, 0);
    this.caMoyenParJour = points.length ? this.caTotal / points.length : 0;
    const meilleur = points.length ? points.reduce((a, b) => (b.valeur > a.valeur ? b : a)) : null;
    this.meilleureJournee = meilleur && meilleur.valeur > 0 ? { date: meilleur.date, ca: meilleur.valeur } : null;

    if (points.length) {
      const c = courbeMontantsParJour(points);
      this.chartCA = c.data;
      this.optionsCA = c.options;
    } else {
      this.chartCA = null;
    }

    const resa = (data.reservationsParJour || []).map(d => ({ date: d.date, valeur: Number(d.count) || 0 }));
    this.nbPointsResa = resa.length;
    if (resa.length && resa.some(r => r.valeur > 0)) {
      const b = colonnesParJour(resa, SERIES[2], (v) => `${v} réservation${v > 1 ? 's' : ''}`);
      this.chartResa = b.data;
      this.optionsResa = b.options;
    } else {
      this.chartResa = null;
    }

    this.construireAgents(data);
  }

  /** Une courbe à un seul point ne raconte rien : on bascule sur un état dédié. */
  get afficheCourbeCA(): boolean {
    return !!this.chartCA && this.nbPointsCA >= 2;
  }

  get caToutNul(): boolean {
    return this.caTotal <= 0;
  }

  get afficheBarresResa(): boolean {
    return !!this.chartResa && this.nbPointsResa >= 1;
  }

  // ==========================================================================
  //  État du parc (temps réel)
  // ==========================================================================

  private construireEtatParc(): void {
    const compte = (etat: string) => this.chambres.filter(c => c.etat === etat).length;
    this.segmentsEtat = [
      { cle: 'LIBRE', label: 'Libres', valeur: compte('LIBRE'), couleur: ETAT_SERIES['LIBRE'] },
      { cle: 'OCCUPEE', label: 'Occupées', valeur: compte('OCCUPEE'), couleur: ETAT_SERIES['OCCUPEE'] },
      { cle: 'A_NETTOYER', label: 'À nettoyer', valeur: compte('A_NETTOYER'), couleur: ETAT_SERIES['A_NETTOYER'] },
      { cle: 'EN_MAINTENANCE', label: 'En maintenance', valeur: compte('EN_MAINTENANCE'), couleur: ETAT_SERIES['EN_MAINTENANCE'] }
    ];
  }

  get segmentsVisibles(): SegmentEtat[] {
    return this.segmentsEtat.filter(s => s.valeur > 0);
  }

  get totalChambres(): number { return this.chambres.length; }
  get chambresLibres(): number { return this.chambres.filter(c => c.etat === 'LIBRE').length; }
  get chambresOccupees(): number { return this.chambres.filter(c => c.etat === 'OCCUPEE').length; }
  get chambresANettoyer(): ChambreResponse[] { return this.chambres.filter(c => c.etat === 'A_NETTOYER'); }

  /**
   * Taux d'occupation calculé sur le parc chargé ici, et non repris du KPI :
   * la carte annonce « en temps réel », le chiffre doit donc suivre les chambres
   * affichées à côté et non la période sélectionnée.
   */
  get tauxOccupation(): number {
    return this.totalChambres > 0
      ? Math.round((this.chambresOccupees / this.totalChambres) * 1000) / 10
      : 0;
  }

  // ==========================================================================
  //  Agents
  // ==========================================================================

  private construireAgents(data: KpiResponse): void {
    const lignes = (data.performanceAgents || [])
      .map(a => ({ id: a.agentId, n: Number(a.nbSejours) || 0, ca: Number(a.ca) || 0 }))
      .sort((a, b) => b.ca - a.ca);
    const max = Math.max(...lignes.map(l => l.ca), 1);

    this.agents = lignes.map((l, i) => {
      const name = this.agentNames[l.id] || `Agent #${l.id}`;
      return {
        name,
        initials: name.split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase(),
        count: `${l.n} séjour${l.n > 1 ? 's' : ''}`,
        ca: montantComplet(l.ca),
        part: (l.ca / max) * 100,
        // Emphase : le meilleur en bordeaux plein, les autres en gris de retrait.
        couleur: i === 0 ? SERIES[0] : '#d9c9cd'
      };
    });
  }

  // ==========================================================================
  //  Vue agent
  // ==========================================================================

  get agentTodos(): Todo[] {
    const todos: Todo[] = [];
    this.chambresANettoyer.slice(0, 3).forEach(c => {
      todos.push({
        icon: 'ti-spray', label: `Chambre ${c.numero} à nettoyer`, tag: 'Entretien',
        bg: '#fdf4e3', cls: 'c-amber'
      });
    });
    this.departsJour.slice(0, 3).forEach(s => {
      todos.push({
        icon: 'ti-logout-2', label: `Départ · ${s.clientNom || 'Client'} (ch. ${s.chambreNumero})`, tag: 'Aujourd\'hui',
        bg: '#fbeef1', cls: 'c-wine'
      });
    });
    if (this.arriveesDuJour.length) {
      todos.push({
        icon: 'ti-login-2', label: `${this.arriveesDuJour.length} arrivée(s) à enregistrer`, tag: 'Réception',
        bg: '#eef7f2', cls: 'c-green'
      });
    }
    return todos;
  }

  // ==========================================================================
  //  Formatage
  // ==========================================================================

  formatCurrency(v: number | undefined | null): string {
    return montantComplet(Number(v) || 0);
  }

  formatPct(v: number | undefined | null): string {
    return `${(Number(v) || 0).toFixed(1).replace('.', ',')} %`;
  }

  formatNombre(v: number | undefined | null): string {
    return new Intl.NumberFormat('fr-FR').format(Number(v) || 0);
  }

  labelJour(isoDate: string): string {
    return isoDate ? new Date(`${isoDate}T00:00:00`).toLocaleDateString('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long'
    }) : '';
  }
}
