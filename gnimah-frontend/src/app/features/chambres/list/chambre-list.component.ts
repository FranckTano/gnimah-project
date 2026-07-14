import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ChambreService } from '../../../core/services/chambre.service';
import {
  ChambreRequest,
  ChambreResponse,
  EtatChambre,
  TypeChambre,
  ETAT_CHAMBRE_LABELS
} from '../../../core/models/chambre.model';

@Component({
  selector: 'app-chambre-list',
  standalone: false,
  templateUrl: './chambre-list.component.html',
  styleUrl: './chambre-list.component.scss'
})
export class ChambreListComponent implements OnInit {
  chambres: ChambreResponse[] = [];
  loading = false;
  dialogVisible = false;
  dialogTitle = 'Nouvelle chambre';
  editingId: number | null = null;
  saving = false;

  form!: FormGroup;

  typeOptions: { label: string; value: TypeChambre }[] = [
    { label: 'Standard', value: 'STANDARD' },
    { label: 'Supérieure', value: 'SUPERIEURE' },
    { label: 'Deluxe', value: 'DELUXE' },
    { label: 'Suite', value: 'SUITE' },
    { label: 'Familiale', value: 'FAMILIALE' }
  ];

  etatOptions: { label: string; value: EtatChambre }[] = [
    { label: 'Libre', value: 'LIBRE' },
    { label: 'Occupée', value: 'OCCUPEE' },
    { label: 'À nettoyer', value: 'A_NETTOYER' },
    { label: 'En maintenance', value: 'EN_MAINTENANCE' },
    { label: 'Hors service', value: 'HORS_SERVICE' }
  ];

  etatLabels: Record<string, string> = ETAT_CHAMBRE_LABELS;

  constructor(
    private chambreService: ChambreService,
    private messageService: MessageService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadChambres();
  }

  initForm(): void {
    this.form = this.fb.group({
      numero: ['', Validators.required],
      type: ['STANDARD', Validators.required],
      capacite: [1, [Validators.required, Validators.min(1)]],
      tarifPassage: [0, [Validators.required, Validators.min(0)]],
      tarifNuitee: [0, [Validators.required, Validators.min(0)]],
      etat: ['LIBRE'],
      etage: [1],
      description: [''],
      equipements: ['']
    });
  }

  loadChambres(): void {
    this.loading = true;
    this.chambreService.findAll().subscribe({
      next: (data) => {
        this.chambres = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Chargement impossible' });
      }
    });
  }

  openNew(): void {
    this.editingId = null;
    this.dialogTitle = 'Nouvelle chambre';
    this.form.reset({ type: 'STANDARD', etat: 'LIBRE', capacite: 1, tarifPassage: 0, tarifNuitee: 0, etage: 1 });
    this.dialogVisible = true;
  }

  openEdit(chambre: ChambreResponse): void {
    this.editingId = chambre.id;
    this.dialogTitle = 'Modifier la chambre';
    this.form.patchValue({
      numero: chambre.numero,
      type: chambre.type,
      capacite: chambre.capacite,
      tarifPassage: chambre.tarifPassage,
      tarifNuitee: chambre.tarifNuitee,
      etat: chambre.etat,
      etage: chambre.etage,
      description: chambre.description,
      equipements: chambre.equipements
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const request: ChambreRequest = this.form.value;

    const obs = this.editingId
      ? this.chambreService.update(this.editingId, request)
      : this.chambreService.create(request);

    obs.subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: this.editingId ? 'Chambre modifiée' : 'Chambre créée'
        });
        this.dialogVisible = false;
        this.saving = false;
        this.loadChambres();
      },
      error: () => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Opération échouée' });
      }
    });
  }

  isFieldInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-CI', {
      style: 'currency', currency: 'XOF', maximumFractionDigits: 0
    }).format(amount || 0);
  }

  getEtatSeverity(etat: EtatChambre): string {
    const map: Record<EtatChambre, string> = {
      LIBRE: 'success',
      OCCUPEE: 'danger',
      A_NETTOYER: 'warn',
      EN_MAINTENANCE: 'info',
      HORS_SERVICE: 'secondary'
    };
    return map[etat] || 'secondary';
  }
}
