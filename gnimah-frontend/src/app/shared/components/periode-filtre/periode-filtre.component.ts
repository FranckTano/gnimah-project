import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Periode, Preset, PRESETS, periodeDepuisPreset } from '../../periode';

/**
 * Barre de filtres de période — une seule rangée, au-dessus de tout ce qu'elle
 * cadre. Partagée par le tableau de bord et la page Statistiques & KPI pour que
 * les deux écrans se pilotent exactement de la même façon.
 */
@Component({
  selector: 'app-periode-filtre',
  standalone: false,
  templateUrl: './periode-filtre.component.html',
  styleUrl: './periode-filtre.component.scss'
})
export class PeriodeFiltreComponent implements OnInit {
  /** Raccourci actif au premier affichage. */
  @Input() presetInitial: Preset = 'jour';
  /** Grise la barre et anime le bouton pendant un rechargement. */
  @Input() loading = false;
  /** Émis à l'initialisation puis à chaque changement validé. */
  @Output() periode = new EventEmitter<Periode>();

  presets = PRESETS;
  presetActif: Preset = 'jour';
  debut = '';
  fin = '';

  ngOnInit(): void {
    this.appliquerPreset(this.presetInitial, false);
    // L'émission initiale part en micro-tâche : `ngOnInit` s'exécute au milieu
    // du cycle de détection du parent, et modifier son état à cet instant
    // déclenche NG0100 (« expression modifiée après vérification ») sur tout ce
    // qu'il affiche déjà de la période. Un tour de boucle plus tard, le parent a
    // fini son rendu et Angular planifie proprement un nouveau cycle.
    Promise.resolve().then(() => this.emettre());
  }

  appliquerPreset(preset: Preset, emettre = true): void {
    const p = periodeDepuisPreset(preset);
    this.presetActif = p.preset;
    this.debut = p.debut;
    this.fin = p.fin;
    if (emettre) this.emettre();
  }

  /** Une saisie manuelle bascule le sélecteur sur « Personnalisé ». */
  onDateManuelle(): void {
    this.presetActif = 'perso';
  }

  /**
   * Le backend ne sait pas répondre à une plage inversée : on remet les bornes
   * dans l'ordre plutôt que de renvoyer une période vide sans explication.
   */
  valider(): void {
    if (this.debut && this.fin && this.debut > this.fin) {
      [this.debut, this.fin] = [this.fin, this.debut];
    }
    this.emettre();
  }

  reinitialiser(): void {
    this.appliquerPreset('jour');
  }

  private emettre(): void {
    this.periode.emit({ debut: this.debut, fin: this.fin, preset: this.presetActif });
  }
}
