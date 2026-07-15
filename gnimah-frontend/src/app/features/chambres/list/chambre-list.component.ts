import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ChambreService } from '../../../core/services/chambre.service';
import { PageHeaderService } from '../../../core/services/page-header.service';
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

  // Filters
  search = '';
  filtreType: TypeChambre | '' = '';
  filtreEtage: number | '' = '';
  filtreEtat: EtatChambre | '' = '';

  form!: FormGroup;

  typeOptions: { label: string; value: TypeChambre }[] = [
    { label: 'Standard', value: 'STANDARD' },
    { label: 'Supérieure', value: 'SUPERIEURE' },
    { label: 'Deluxe', value: 'DELUXE' },
    { label: 'Suite', value: 'SUITE' },
    { label: 'Familiale', value: 'FAMILIALE' }
  ];

  vueOptions = [
    { label: '—', value: '' },
    { label: 'Mer', value: 'MER' },
    { label: 'Jardin', value: 'JARDIN' },
    { label: 'Piscine', value: 'PISCINE' },
    { label: 'Rue', value: 'RUE' },
    { label: 'Cour intérieure', value: 'COUR' }
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
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Gestion des chambres', 'Créer, modifier, filtrer et activer les chambres de l\'hôtel');
    this.initForm();
    this.loadChambres();
  }

  initForm(): void {
    this.form = this.fb.group({
      numero: ['', Validators.required],
      type: ['STANDARD', Validators.required],
      capacite: [2, [Validators.required, Validators.min(1)]],
      tarifPassage: [0, [Validators.required, Validators.min(0)]],
      tarifNuitee: [0, [Validators.required, Validators.min(0)]],
      etat: ['LIBRE'],
      etage: [0],
      vue: [''],
      description: [''],
      equipements: [''],
      observations: ['']
    });
  }

  loadChambres(): void {
    this.loading = true;
    this.chambreService.findAllAdmin().subscribe({
      next: (data) => { this.chambres = data; this.loading = false; },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Chargement impossible' });
      }
    });
  }

  get etages(): number[] {
    return [...new Set(this.chambres.map(c => c.etage))].sort((a, b) => a - b);
  }

  get filtered(): ChambreResponse[] {
    const q = this.search.trim().toLowerCase();
    return this.chambres.filter(c => {
      if (q && !c.numero.toLowerCase().includes(q) && !(c.equipements || '').toLowerCase().includes(q)) return false;
      if (this.filtreType && c.type !== this.filtreType) return false;
      if (this.filtreEtage !== '' && c.etage !== this.filtreEtage) return false;
      if (this.filtreEtat && c.etat !== this.filtreEtat) return false;
      return true;
    });
  }

  resetFiltres(): void {
    this.search = '';
    this.filtreType = '';
    this.filtreEtage = '';
    this.filtreEtat = '';
  }

  openNew(): void {
    this.editingId = null;
    this.dialogTitle = 'Nouvelle chambre';
    this.form.reset({ type: 'STANDARD', etat: 'LIBRE', capacite: 2, tarifPassage: 0, tarifNuitee: 0, etage: 0, vue: '' });
    this.dialogVisible = true;
  }

  openEdit(chambre: ChambreResponse): void {
    this.editingId = chambre.id;
    this.dialogTitle = `Modifier la chambre ${chambre.numero}`;
    this.form.patchValue({
      numero: chambre.numero,
      type: chambre.type,
      capacite: chambre.capacite,
      tarifPassage: chambre.tarifPassage,
      tarifNuitee: chambre.tarifNuitee,
      etat: chambre.etat,
      etage: chambre.etage,
      vue: chambre.vue || '',
      description: chambre.description,
      equipements: chambre.equipements,
      observations: chambre.observations
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
      error: (err) => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err?.error?.message || 'Opération échouée' });
      }
    });
  }

  toggleActif(chambre: ChambreResponse): void {
    this.confirmationService.confirm({
      message: chambre.actif
        ? `Désactiver la chambre ${chambre.numero} ? Elle disparaîtra du plan des chambres et des sélections de check-in.`
        : `Réactiver la chambre ${chambre.numero} ?`,
      header: chambre.actif ? 'Désactiver la chambre' : 'Réactiver la chambre',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.chambreService.toggleActif(chambre.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: chambre.actif ? 'Chambre désactivée' : 'Chambre réactivée' });
            this.loadChambres();
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Opération impossible' })
        });
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

  stateTone(etat: EtatChambre): string {
    return { LIBRE: 'green', OCCUPEE: 'wine', A_NETTOYER: 'amber', EN_MAINTENANCE: 'slate', HORS_SERVICE: 'gray' }[etat] || 'gray';
  }

  floorLabel(etage: number): string {
    if (etage === 0) return 'Rez-de-chaussée';
    if (etage === 1) return '1er étage';
    return `${etage}e étage`;
  }
}
