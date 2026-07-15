import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntretienService } from '../../core/services/entretien.service';
import { ChambreService } from '../../core/services/chambre.service';
import { TacheEntretienResponse } from '../../core/models/entretien.model';
import { ChambreResponse } from '../../core/models/chambre.model';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PageHeaderService } from '../../core/services/page-header.service';

interface KanbanColumn {
  statut: string;
  title: string;
  tone: string;
  tasks: TacheEntretienResponse[];
}

@Component({
  selector: 'app-entretien',
  standalone: false,
  templateUrl: './entretien.component.html',
  styleUrl: './entretien.component.scss'
})
export class EntretienComponent implements OnInit {
  taches: TacheEntretienResponse[] = [];
  chambres: ChambreResponse[] = [];
  loading = false;
  showDialog = false;
  saving = false;
  form!: FormGroup;

  typeOptions = [
    { label: 'Nettoyage', value: 'NETTOYAGE' },
    { label: 'Maintenance', value: 'MAINTENANCE' },
    { label: 'Inspection', value: 'INSPECTION' },
    { label: 'Réparation', value: 'REPARATION' }
  ];

  prioriteOptions = [
    { label: 'Normale', value: 'NORMALE' },
    { label: 'Urgente', value: 'URGENTE' }
  ];

  constructor(
    private fb: FormBuilder,
    private entretienService: EntretienService,
    private chambreService: ChambreService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Housekeeping', "Suivi de l'entretien des chambres");
    this.form = this.fb.group({
      chambreId: [null, Validators.required],
      type: ['NETTOYAGE', Validators.required],
      description: ['', Validators.required],
      assigneA: [''],
      priorite: ['NORMALE']
    });
    this.load();
    this.chambreService.findAll().subscribe({ next: (d) => { this.chambres = d; } });
  }

  load(): void {
    this.loading = true;
    this.entretienService.findAll().subscribe({
      next: (data) => { this.taches = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get columns(): KanbanColumn[] {
    return [
      { statut: 'A_FAIRE', title: 'À faire', tone: 'c-gold', tasks: this.taches.filter(t => t.statut === 'A_FAIRE') },
      { statut: 'EN_COURS', title: 'En cours', tone: 'c-slate', tasks: this.taches.filter(t => t.statut === 'EN_COURS') },
      { statut: 'TERMINE', title: 'Terminé', tone: 'c-green', tasks: this.taches.filter(t => t.statut === 'TERMINE') }
    ];
  }

  initials(name: string | null): string {
    return (name || 'NA').split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  openNew(): void {
    this.form.reset({ type: 'NETTOYAGE', priorite: 'NORMALE' });
    this.showDialog = true;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.entretienService.create(this.form.value).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Tâche créée', detail: 'Tâche d\'entretien enregistrée' });
        this.showDialog = false;
        this.saving = false;
        this.load();
      },
      error: () => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec de l\'enregistrement' });
      }
    });
  }

  changerStatut(t: TacheEntretienResponse, statut: string): void {
    this.entretienService.updateStatut(t.id, statut).subscribe({
      next: () => { this.load(); },
      error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Changement de statut impossible' })
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f); return !!(c && c.invalid && c.touched);
  }
}
