import { Component, OnInit } from '@angular/core';
import { KpiService } from '../../core/services/kpi.service';
import { UtilisateurService } from '../../core/services/utilisateur.service';
import { PageHeaderService } from '../../core/services/page-header.service';
import { MediaService } from '../../core/services/media.service';
import { KpiResponse, MODE_PAIEMENT_LABELS } from '../../core/models/kpi.model';
import { SERIES, ETAT_SERIES, RAMPE_BORDEAUX, labelJourLong, montantComplet } from '../../shared/chart-theme';
import { PLUGIN_CURSEUR, courbeMontantsParJour, colonnesParJour } from '../../shared/chart-builders';
import { Periode, libellePeriode, nbJours } from '../../shared/periode';

/** Une ligne de la barre empilée « état du parc ». */
interface SegmentEtat {
  cle: string;
  label: string;
  valeur: number;
  couleur: string;
  part: number;
}

/** Une barre horizontale étiquetée (modes de paiement, agents). */
interface BarreLibellee {
  label: string;
  sous: string;
  valeur: string;
  part: number;
  couleur: string;
}

@Component({
  selector: 'app-kpi',
  standalone: false,
  templateUrl: './kpi.component.html',
  styleUrl: './kpi.component.scss'
})
export class KpiComponent implements OnInit {
  kpi: KpiResponse | null = null;
  loading = false;
  /** Vrai uniquement au tout premier chargement : ensuite on garde le rendu précédent. */
  premierChargement = true;

  periode: Periode = { debut: '', fin: '', preset: 'jour' };
  agentNames: Record<number, string> = {};

  // ---- Graphiques Chart.js -------------------------------------------------
  pluginCurseur = PLUGIN_CURSEUR;
  chartCA: any = null;
  optionsCA: any = null;
  chartReservations: any = null;
  optionsReservations: any = null;

  private nbPointsCA = 0;
  private nbPointsResa = 0;

  // ---- Blocs dérivés -------------------------------------------------------
  segmentsEtat: SegmentEtat[] = [];
  modesPaiement: BarreLibellee[] = [];
  agents: BarreLibellee[] = [];
  indicateurs: { label: string; valeur: string; aide: string }[] = [];

  caTotal = 0;
  caMoyenParJour = 0;
  meilleureJournee: { date: string; ca: number } | null = null;
  partEncaissee = 0;
  totalResa = 0;

  afficherTableau = false;
  photoBandeau = '';

  constructor(
    private kpiService: KpiService,
    private utilisateurService: UtilisateurService,
    private pageHeaderService: PageHeaderService,
    private media: MediaService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Statistiques & KPI', 'Analyse de la performance de la résidence');
    this.photoBandeau = this.media.heros.src;

    this.utilisateurService.findAll().subscribe(users => {
      users.forEach(u => this.agentNames[u.id] = `${u.prenom} ${u.nom}`.trim());
      if (this.kpi) this.construireAgents(this.kpi);
    });
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

  /** Appelé par la barre de filtres, à l'initialisation puis à chaque changement. */
  onPeriode(p: Periode): void {
    this.periode = p;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.kpiService.getDashboard(this.periode.debut || undefined, this.periode.fin || undefined).subscribe({
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
    this.construireChartCA(data);
    this.construireChartReservations(data);
    this.construireEtatParc(data);
    this.construireModesPaiement(data);
    this.construireAgents(data);
    this.construireIndicateurs(data);

    const ca = Number(data.chiffreAffaires) || 0;
    this.partEncaissee = ca > 0 ? Math.min(100, (Number(data.encaissements) || 0) / ca * 100) : 0;
    this.totalResa = Number(data.reservationsTotal) || 0;
  }

  // ==========================================================================
  //  Chiffre d'affaires par jour — courbe en aire
  // ==========================================================================

  private construireChartCA(data: KpiResponse): void {
    const points = (data.caParJour || []).map(d => ({ date: d.date, valeur: Number(d.ca) || 0 }));
    this.nbPointsCA = points.length;
    this.caTotal = points.reduce((s, p) => s + p.valeur, 0);
    this.caMoyenParJour = points.length ? this.caTotal / points.length : 0;

    const meilleur = points.length ? points.reduce((a, b) => (b.valeur > a.valeur ? b : a)) : null;
    this.meilleureJournee = meilleur && meilleur.valeur > 0 ? { date: meilleur.date, ca: meilleur.valeur } : null;

    if (!points.length) { this.chartCA = null; return; }
    const c = courbeMontantsParJour(points);
    this.chartCA = c.data;
    this.optionsCA = c.options;
  }

  /**
   * Une courbe à un seul point ne raconte rien : sur une période d'une journée on
   * bascule sur un état dédié qui affiche le montant et propose d'élargir.
   */
  get afficheCourbeCA(): boolean {
    return !!this.chartCA && this.nbPointsCA >= 2;
  }

  get caToutNul(): boolean {
    return this.caTotal <= 0;
  }

  get afficheBarresResa(): boolean {
    return !!this.chartReservations && this.nbPointsResa >= 1;
  }

  // ==========================================================================
  //  Réservations par jour — colonnes
  // ==========================================================================

  private construireChartReservations(data: KpiResponse): void {
    const points = (data.reservationsParJour || []).map(d => ({ date: d.date, valeur: Number(d.count) || 0 }));
    this.nbPointsResa = points.length;
    if (!points.length || points.every(p => p.valeur === 0)) { this.chartReservations = null; return; }

    const b = colonnesParJour(points, SERIES[2], (v) => `${v} réservation${v > 1 ? 's' : ''}`);
    this.chartReservations = b.data;
    this.optionsReservations = b.options;
  }

  // ==========================================================================
  //  État du parc — barre empilée + légende chiffrée
  // ==========================================================================

  private construireEtatParc(data: KpiResponse): void {
    const brut = [
      { cle: 'LIBRE', label: 'Libres', valeur: Number(data.chambresLibres) || 0 },
      { cle: 'OCCUPEE', label: 'Occupées', valeur: Number(data.chambresOccupees) || 0 },
      { cle: 'A_NETTOYER', label: 'À nettoyer', valeur: Number(data.chambresANettoyer) || 0 },
      { cle: 'EN_MAINTENANCE', label: 'En maintenance', valeur: Number(data.chambresEnMaintenance) || 0 }
    ];
    const total = brut.reduce((s, b) => s + b.valeur, 0) || 1;
    this.segmentsEtat = brut.map(b => ({
      ...b,
      couleur: ETAT_SERIES[b.cle],
      part: (b.valeur / total) * 100
    }));
  }

  /** Segments non vides seulement : un segment à 0 % ne doit pas manger d'espace. */
  get segmentsVisibles(): SegmentEtat[] {
    return this.segmentsEtat.filter(s => s.valeur > 0);
  }

  // ==========================================================================
  //  Modes de paiement & agents — barres HTML étiquetées
  // ==========================================================================

  private construireModesPaiement(data: KpiResponse): void {
    const lignes = (data.repartitionModePaiement || [])
      .map(m => ({
        mode: MODE_PAIEMENT_LABELS[m.mode] || m.mode,
        count: Number(m.count) || 0,
        montant: Number(m.montant) || 0
      }))
      .sort((a, b) => b.montant - a.montant);

    const max = Math.max(...lignes.map(l => l.montant), 1);
    this.modesPaiement = lignes.map((l, i) => ({
      label: l.mode,
      sous: `${l.count} opération${l.count > 1 ? 's' : ''}`,
      valeur: montantComplet(l.montant),
      part: (l.montant / max) * 100,
      // Identité par mode : la couleur suit le moyen de paiement, pas son rang.
      couleur: SERIES[i % SERIES.length]
    }));
  }

  private construireAgents(data: KpiResponse): void {
    const lignes = (data.performanceAgents || [])
      .map(a => ({ id: a.agentId, n: Number(a.nbSejours) || 0, ca: Number(a.ca) || 0 }))
      .sort((a, b) => b.ca - a.ca);
    const max = Math.max(...lignes.map(l => l.ca), 1);

    this.agents = lignes.map((l, i) => ({
      label: this.agentName(l.id),
      sous: `${l.n} séjour${l.n > 1 ? 's' : ''}`,
      valeur: montantComplet(l.ca),
      part: (l.ca / max) * 100,
      // Emphase : le meilleur agent en bordeaux plein, les autres en gris de retrait.
      couleur: i === 0 ? SERIES[0] : '#d9c9cd'
    }));
  }

  // ==========================================================================
  //  Indicateurs hôteliers — libellés en clair pour la direction
  // ==========================================================================

  private construireIndicateurs(data: KpiResponse): void {
    this.indicateurs = [
      {
        label: "Taux d'occupation",
        valeur: this.formatPct(data.tauxOccupation),
        aide: 'Part des chambres actuellement occupées sur tout le parc.'
      },
      {
        label: 'Tarif moyen par chambre occupée',
        valeur: this.formatMontant(data.adr),
        aide: "Ce que rapporte en moyenne une chambre effectivement louée (ADR)."
      },
      {
        label: 'Revenu par chambre disponible',
        valeur: this.formatMontant(data.revPar),
        aide: "Chiffre d'affaires ramené à toutes les chambres, occupées ou non (RevPAR)."
      },
      {
        label: 'Durée moyenne de séjour',
        valeur: `${(Number(data.alos) || 0).toFixed(1).replace('.', ',')} nuit${(Number(data.alos) || 0) > 1 ? 's' : ''}`,
        aide: 'Nombre de nuits que passe en moyenne un client.'
      },
      {
        label: 'Panier moyen par séjour',
        valeur: this.formatMontant(data.panierMoyen),
        aide: 'Montant facturé en moyenne pour un séjour.'
      },
      {
        label: 'Taux de conversion',
        valeur: this.formatPct(data.tauxConversion),
        aide: 'Part des réservations transformées en séjour réel.'
      }
    ];
  }

  // ==========================================================================
  //  Entonnoir réservations → séjours (rampe ordinale bordeaux)
  // ==========================================================================

  get etapesEntonnoir(): { label: string; valeur: number; part: number; couleur: string }[] {
    if (!this.kpi) return [];
    const resa = Number(this.kpi.reservationsTotal) || 0;
    const sejours = Number(this.kpi.totalSejours) || 0;
    const base = Math.max(resa, sejours, 1);
    return [
      { label: 'Réservations reçues', valeur: resa, part: (resa / base) * 100, couleur: RAMPE_BORDEAUX[0] },
      { label: 'Séjours réalisés', valeur: sejours, part: (sejours / base) * 100, couleur: RAMPE_BORDEAUX[2] }
    ];
  }

  // ==========================================================================
  //  Vue tableau — jumelle accessible des graphiques
  // ==========================================================================

  get lignesTableau(): { date: string; ca: string; resa: number }[] {
    if (!this.kpi) return [];
    const resa = new Map((this.kpi.reservationsParJour || []).map(r => [r.date, Number(r.count) || 0]));
    return (this.kpi.caParJour || []).map(d => ({
      date: labelJourLong(d.date),
      ca: montantComplet(Number(d.ca) || 0),
      resa: resa.get(d.date) ?? 0
    }));
  }

  // ==========================================================================
  //  Utilitaires d'affichage
  // ==========================================================================

  agentName(id: number): string {
    return this.agentNames[id] || `Agent #${id}`;
  }

  agentInitials(id: number): string {
    return this.agentName(id).split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  formatMontant(v: number): string {
    return montantComplet(Number(v) || 0);
  }

  formatPct(v: number): string {
    return `${(Number(v) || 0).toFixed(1).replace('.', ',')} %`;
  }

  formatNombre(v: number): string {
    return new Intl.NumberFormat('fr-FR').format(Number(v) || 0);
  }

  labelJour(iso: string): string {
    return labelJourLong(iso);
  }
}
