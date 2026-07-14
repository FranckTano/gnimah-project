import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EvenementService } from '../../core/services/evenement.service';
import { EvenementResponse } from '../../core/models/evenement.model';
import { MessageService, ConfirmationService } from 'primeng/api';

@Component({
  selector: 'app-evenements',
  standalone: false,
  templateUrl: './evenements.component.html',
  styleUrl: './evenements.component.scss'
})
export class EvenementsComponent implements OnInit {
  evenements: EvenementResponse[] = [];
  loading = false;
  showDialog = false;
  editingId: number | null = null;
  saving = false;
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private evenementService: EvenementService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      titre: ['', Validators.required],
      description: [''],
      dateDebut: [null, Validators.required],
      dateFin: [null, Validators.required],
      lieu: ['', Validators.required],
      nombreParticipants: [0],
      montant: [0]
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.evenementService.findAll().subscribe({
      next: (data) => { this.evenements = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openNew(): void {
    this.editingId = null;
    this.form.reset({ nombreParticipants: 0, montant: 0 });
    this.showDialog = true;
  }

  openEdit(ev: EvenementResponse): void {
    this.editingId = ev.id;
    this.form.patchValue({
      ...ev,
      dateDebut: new Date(ev.dateDebut),
      dateFin: new Date(ev.dateFin)
    });
    this.showDialog = true;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const v = this.form.value;
    const req = {
      ...v,
      dateDebut: v.dateDebut instanceof Date ? v.dateDebut.toISOString() : v.dateDebut,
      dateFin: v.dateFin instanceof Date ? v.dateFin.toISOString() : v.dateFin
    };
    const obs = this.editingId
      ? this.evenementService.update(this.editingId, req)
      : this.evenementService.create(req);
    obs.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: this.editingId ? 'Événement modifié' : 'Événement créé' });
        this.showDialog = false; this.saving = false; this.load();
      },
      error: () => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Opération échouée' });
      }
    });
  }

  delete(ev: EvenementResponse): void {
    this.confirmationService.confirm({
      message: `Supprimer l'événement "${ev.titre}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-trash',
      accept: () => {
        this.evenementService.delete(ev.id).subscribe({
          next: () => { this.messageService.add({ severity: 'warn', summary: 'Supprimé', detail: 'Événement supprimé' }); this.load(); },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Suppression impossible' })
        });
      }
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f); return !!(c && c.invalid && c.touched);
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
  }
}
