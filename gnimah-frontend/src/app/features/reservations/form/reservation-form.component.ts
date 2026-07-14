import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { ClientService } from '../../../core/services/client.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { ClientResponse } from '../../../core/models/client.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { MessageService } from 'primeng/api';

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
  today = new Date();

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
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      clientId: [null, Validators.required],
      chambreId: [null],
      typeChambre: [null],
      dateArrivee: [null, Validators.required],
      dateDepart: [null, Validators.required],
      acompte: [0],
      notes: ['']
    });
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.clientService.search(undefined, 0, 500).subscribe({ next: (d) => { this.clients = d.content; } });
    this.chambreService.findAll().subscribe({ next: (d) => { this.chambres = d; this.loading = false; }, error: () => { this.loading = false; } });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const v = this.form.value;
    const request = {
      ...v,
      dateArrivee: v.dateArrivee instanceof Date ? v.dateArrivee.toISOString().split('T')[0] : v.dateArrivee,
      dateDepart: v.dateDepart instanceof Date ? v.dateDepart.toISOString().split('T')[0] : v.dateDepart
    };
    this.reservationService.create(request).subscribe({
      next: (res) => {
        this.messageService.add({ severity: 'success', summary: 'Réservation créée', detail: `N° ${res.numeroReservation}` });
        this.saving = false;
        this.router.navigate(['/reservations']);
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err?.error?.message || 'Échec de la réservation' });
      }
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c && c.invalid && c.touched);
  }
}
