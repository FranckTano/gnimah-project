import { Component, OnInit } from '@angular/core';
import { ComptabiliteService } from '../../core/services/comptabilite.service';
import { CaisseService } from '../../core/services/caisse.service';
import {
  FAMILLE_DEPENSE_ICONS, FAMILLE_DEPENSE_LABELS, LigneRepartition, SyntheseComptable
} from '../../core/models/comptabilite.model';
import { CaisseResponse } from '../../core/models/caisse.model';
import { PageHeaderService } from '../../core/services/page-header.service';
import { Periode, libellePeriode } from '../../shared/periode';
import { MessageService } from 'primeng/api';

/**
 * Compte de résultat de la résidence sur une période : d'où vient l'argent, où
 * il part, ce qu'il en reste. La page tient volontairement sur un seul écran de
 * lecture — le détail se consulte dans les modules qui l'alimentent.
 */
@Component({
  selector: 'app-comptabilite',
  standalone: false,
  templateUrl: './comptabilite.component.html',
  styleUrl: './comptabilite.component.scss'
})
export class ComptabiliteComponent implements OnInit {
  synthese: SyntheseComptable | null = null;
  caisses: CaisseResponse[] = [];
  loading = false;
  periode: Periode = { debut: '', fin: '', preset: 'mois-courant' };

  familleLabels = FAMILLE_DEPENSE_LABELS;
  familleIcons = FAMILLE_DEPENSE_ICONS;

  constructor(
    private comptabiliteService: ComptabiliteService,
    private caisseService: CaisseService,
    private pageHeaderService: PageHeaderService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Comptabilité', 'Recettes, charges et trésorerie de la résidence');
    this.caisseService.findAll().subscribe({ next: (d) => { this.caisses = d; } });
  }

  onPeriode(p: Periode): void {
    this.periode = p;
    this.charger();
  }

  charger(): void {
    if (!this.periode.debut || !this.periode.fin) return;
    this.loading = true;
    this.comptabiliteService.synthese(this.periode.debut, this.periode.fin).subscribe({
      next: (data) => { this.synthese = data; this.loading = false; },
      error: () => {
        this.loading = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: 'La synthèse comptable n\'a pas pu être chargée'
        });
      }
    });
  }

  get libelle(): string {
    return libellePeriode(this.periode);
  }

  get resultatPositif(): boolean {
    return (this.synthese?.resultat ?? 0) >= 0;
  }

  /** Total des soldes de toutes les caisses — argent physiquement détenu. */
  get soldeCaisses(): number {
    return this.caisses.reduce((t, c) => t + (c.solde || 0), 0);
  }

  /**
   * Largeur de barre d'une ligne de répartition, relative au plus gros poste.
   * Rapportée au total, les petits postes seraient des traits invisibles.
   */
  largeur(ligne: LigneRepartition, lignes: LigneRepartition[]): number {
    const max = Math.max(...lignes.map(l => l.montant), 0);
    if (max <= 0) return 0;
    return Math.max(3, Math.round((ligne.montant / max) * 100));
  }

  /**
   * Hauteur d'une colonne du graphique journalier. Recettes et charges partagent
   * la même échelle — sinon deux barres de hauteur égale représenteraient des
   * montants sans rapport et la courbe deviendrait mensongère.
   */
  hauteurBarre(valeur: number): number {
    const jours = this.synthese?.evolutionJournaliere ?? [];
    const max = Math.max(...jours.map(j => Math.max(j.recettes, j.depenses)), 0);
    if (max <= 0 || !valeur) return 0;
    return Math.max(3, Math.round((valeur / max) * 100));
  }

  formatCourt(v: number | null | undefined): string {
    const n = v || 0;
    if (Math.abs(n) >= 1_000_000) return `${(n / 1_000_000).toFixed(1).replace('.0', '')} M`;
    if (Math.abs(n) >= 1_000) return `${Math.round(n / 1_000)} k`;
    return `${Math.round(n)}`;
  }
}
