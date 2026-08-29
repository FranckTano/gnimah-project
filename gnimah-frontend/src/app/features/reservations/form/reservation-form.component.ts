import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { ClientService } from '../../../core/services/client.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { ClientResponse } from '../../../core/models/client.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { PREAVIS_ANNULATION_HEURES } from '../../../core/models/reservation.model';
import { TYPE_REMISE_OPTIONS } from '../../../core/models/sejour.model';
import { modePaiementOptions } from '../../../core/models/paiement.model';
import { MessageService } from 'primeng/api';
import { PageHeaderService } from '../../../core/services/page-header.service';

/** Part du séjour exigée en acompte par défaut — doit rester alignée sur le backend. */
const TAUX_ACOMPTE_DEFAUT = 0.3;

@Component({
  selector: 'app-reservation-form',
  standalone: false,
  templateUrl: './reservation-form.component.html',
  styleUrl: './reservation-form.component.scss'
})
export class ReservationFormComponent implements OnInit {
  form!: FormGroup;
  clients: ClientResponse[] = [];
  chambres: ChambreResponse[] = [];
  saving = false;
  loading = false;
  today = new Date().toISOString().split('T')[0];

  preavisHeures = PREAVIS_ANNULATION_HEURES;
  typeRemiseOptions = TYPE_REMISE_OPTIONS;
  modeOptions = modePaiementOptions;

  typesChambres = [
    { label: 'Standard', value: 'STANDARD' },
    { label: 'Supérieure', value: 'SUPERIEURE' },
    { label: 'Deluxe', value: 'DELUXE' },
    { label: 'Suite', value: 'SUITE' },
    { label: 'Familiale', value: 'FAMILIALE' }
  ];

  constructor(
    private fb: FormBuilder,
    private reservationService: ReservationService,
    private clientService: ClientService,
    private chambreService: ChambreService,
    private messageService: MessageService,
    private router: Router,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Nouvelle réservation', 'Acompte de confirmation et conditions d\'annulation');
    this.form = this.fb.group({
      clientId: [null, Validators.required],
      chambreId: [null],
      typeChambre: [null],
      dateArrivee: ['', Validators.required],
      dateDepart: ['', Validators.required],
      tarifPersonnalise: [null],
      remiseType: [null],
      remiseValeur: [null],
      remiseMotif: [''],
      acompteRequis: [null],
      acompte: [0],
      modeAcompte: ['ESPECES'],
      referenceAcompte: [''],
      notes: ['']
    });
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.clientService.search(undefined, 0, 500).subscribe({ next: (d) => { this.clients = d.content; } });
    this.chambreService.findAll().subscribe({
      next: (d) => { this.chambres = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  /** Vide le tarif négocié au changement de chambre : il se rapportait à l'ancienne. */
  onChambreChange(): void {
    this.form.patchValue({ tarifPersonnalise: null });
  }

  // ------------------------------------------------------------------
  // Calculs d'affichage
  //
  // Le backend refait tous ces calculs et fait autorité. Ici, ils servent
  // uniquement à ce que l'agent annonce le bon prix au client avant de valider.
  // ------------------------------------------------------------------

  get chambreChoisie(): ChambreResponse | undefined {
    const id = this.form?.value?.chambreId;
    return id ? this.chambres.find(c => c.id === id) : undefined;
  }

  get tarifChambre(): string {
    return this.chambreChoisie ? `${this.chambreChoisie.tarifNuitee}` : '';
  }

  get tarifApplique(): number {
    const negocie = Number(this.form?.value?.tarifPersonnalise) || 0;
    if (negocie > 0) return negocie;
    return this.chambreChoisie?.tarifNuitee ?? 0;
  }

  get nbNuits(): number {
    const a = this.form?.value?.dateArrivee;
    const d = this.form?.value?.dateDepart;
    if (!a || !d) return 0;
    const nuits = Math.round((new Date(d).getTime() - new Date(a).getTime()) / 86_400_000);
    return Math.max(nuits, 1);
  }

  get montantBrut(): number {
    return this.tarifApplique * this.nbNuits;
  }

  get remiseMontant(): number {
    const type = this.form?.value?.remiseType;
    const valeur = Number(this.form?.value?.remiseValeur) || 0;
    if (!type || valeur <= 0) return 0;
    const remise = type === 'POURCENTAGE'
      ? Math.round(this.montantBrut * Math.min(valeur, 100) / 100)
      : valeur;
    return Math.min(remise, this.montantBrut);
  }

  get montantPrevu(): number {
    return this.montantBrut - this.remiseMontant;
  }

  get acompteParDefaut(): string {
    return `${Math.round(this.montantPrevu * TAUX_ACOMPTE_DEFAUT)}`;
  }

  get acompteAttendu(): number {
    const saisi = Number(this.form?.value?.acompteRequis) || 0;
    if (saisi > 0) return Math.min(saisi, this.montantPrevu);
    return Math.round(this.montantPrevu * TAUX_ACOMPTE_DEFAUT);
  }

  // ------------------------------------------------------------------

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const v = this.form.value;
    const request = {
      clientId: v.clientId,
      chambreId: v.chambreId,
      typeChambre: v.typeChambre,
      dateArrivee: v.dateArrivee,
      dateDepart: v.dateDepart,
      tarifPersonnalise: Number(v.tarifPersonnalise) > 0 ? Number(v.tarifPersonnalise) : null,
      remiseType: v.remiseType || null,
      remiseValeur: Number(v.remiseValeur) > 0 ? Number(v.remiseValeur) : null,
      remiseMotif: v.remiseMotif || null,
      acompteRequis: Number(v.acompteRequis) > 0 ? Number(v.acompteRequis) : null,
      acompte: Number(v.acompte) > 0 ? Number(v.acompte) : null,
      modeAcompte: v.modeAcompte,
      referenceAcompte: v.referenceAcompte || undefined,
      notes: v.notes
    };

    this.reservationService.create(request).subscribe({
      next: (res) => {
        this.saving = false;
        this.messageService.add({
          severity: 'success',
          summary: res.statut === 'CONFIRMEE' ? 'Réservation confirmée' : 'Réservation enregistrée',
          detail: res.statut === 'CONFIRMEE'
            ? `N° ${res.numeroReservation} — acompte de ${this.formatMontant(res.acompte)} encaissé`
            : `N° ${res.numeroReservation} — en attente d'un acompte de ${this.formatMontant(res.acompteRequis)}`
        });
        this.router.navigate(['/reservations']);
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: err?.error?.message || 'Échec de la réservation'
        });
      }
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c && c.invalid && c.touched);
  }

  formatMontant(v: number | null | undefined): string {
    return new Intl.NumberFormat('fr-CI', {
      style: 'currency', currency: 'XOF', maximumFractionDigits: 0
    }).format(v || 0);
  }
}
