import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntretienService } from '../../core/services/entretien.service';
import { ChambreService } from '../../core/services/chambre.service';
import { UtilisateurService } from '../../core/services/utilisateur.service';
import { EvenementService } from '../../core/services/evenement.service';
import { AuthService } from '../../core/services/auth.service';
import { TacheEntretienResponse } from '../../core/models/entretien.model';
import { ChambreResponse } from '../../core/models/chambre.model';
import { UtilisateurResponse } from '../../core/models/utilisateur.model';
import { EvenementResponse } from '../../core/models/evenement.model';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PageHeaderService } from '../../core/services/page-header.service';

interface KanbanColumn {
  statut: string;
  title: string;
  tone: string;
  tasks: TacheEntretienResponse[];
}

type Cible = 'CHAMBRE' | 'SALLE' | 'EVENEMENT' | 'AUCUNE';

@Component({
  selector: 'app-entretien',
  standalone: false,
  templateUrl: './entretien.component.html',
  styleUrl: './entretien.component.scss'
})
export class EntretienComponent implements OnInit {
  taches: TacheEntretienResponse[] = [];
  chambres: ChambreResponse[] = [];
  agents: UtilisateurResponse[] = [];
  evenements: EvenementResponse[] = [];
  loading = false;
  showDialog = false;
  saving = false;
  form!: FormGroup;
  cible: Cible = 'AUCUNE';

  typeOptions = [
    { label: 'Nettoyage', value: 'NETTOYAGE' },
    { label: 'Maintenance', value: 'MAINTENANCE' },
    { label: 'Inspection', value: 'INSPECTION' },
    { label: 'Réparation', value: 'REPARATION' },
    { label: 'Préparation de salle', value: 'PREPARATION_SALLE' },
    { label: 'Préparation d\'événement', value: 'PREPARATION_EVENEMENT' },
    { label: 'Entretien espace commun', value: 'ENTRETIEN_ESPACE_COMMUN' },
    { label: 'Autre intervention', value: 'AUTRE' }
  ];

  prioriteOptions = [
    { label: 'Normale', value: 'NORMALE' },
    { label: 'Urgente', value: 'URGENTE' }
  ];

  constructor(
    private fb: FormBuilder,
    private entretienService: EntretienService,
    private chambreService: ChambreService,
    private utilisateurService: UtilisateurService,
    private evenementService: EvenementService,
    public authService: AuthService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Housekeeping', "Suivi de l'entretien des chambres");
    this.form = this.fb.group({
      titre: ['', Validators.required],
      type: ['NETTOYAGE', Validators.required],
      description: ['', Validators.required],
      chambreId: [null],
      salle: [''],
      evenementId: [null],
      agentId: [null, Validators.required],
      priorite: ['NORMALE'],
      dateLimiteJour: [''],
      dateLimiteHeure: ['']
    });
    this.load();
    this.chambreService.findAll().subscribe({ next: (d) => { this.chambres = d; } });
    this.utilisateurService.findAll().subscribe({ next: (d) => { this.agents = d.filter(a => a.actif); } });
    this.evenementService.findAll().subscribe({ next: (d) => { this.evenements = d; } });
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

  cibleLabel(t: TacheEntretienResponse): string {
    if (t.chambreNumero) return `Chambre ${t.chambreNumero}`;
    if (t.salle) return t.salle;
    if (t.evenementTitre) return t.evenementTitre;
    return 'Intervention générale';
  }

  cibleIcon(t: TacheEntretienResponse): string {
    if (t.chambreNumero) return 'ti-door';
    if (t.salle) return 'ti-building';
    if (t.evenementTitre) return 'ti-calendar-event';
    return 'ti-clipboard-list';
  }

  isEnRetard(t: TacheEntretienResponse): boolean {
    return !!t.dateLimite && t.statut !== 'TERMINE' && new Date(t.dateLimite).getTime() < Date.now();
  }

  setCible(c: Cible): void {
    this.cible = c;
    this.form.patchValue({ chambreId: null, salle: '', evenementId: null });
  }

  openNew(): void {
    this.cible = 'AUCUNE';
    this.form.reset({ type: 'NETTOYAGE', priorite: 'NORMALE' });
    this.showDialog = true;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const v = this.form.value;
    const request = {
      titre: v.titre,
      type: v.type,
      description: v.description,
      agentId: v.agentId,
      priorite: v.priorite,
      chambreId: this.cible === 'CHAMBRE' ? v.chambreId : null,
      salle: this.cible === 'SALLE' ? v.salle : null,
      evenementId: this.cible === 'EVENEMENT' ? v.evenementId : null,
      dateLimite: v.dateLimiteJour ? `${v.dateLimiteJour}T${v.dateLimiteHeure || '18:00'}:00` : null
    };
    this.entretienService.create(request).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Tâche créée', detail: 'Tâche d\'entretien enregistrée' });
        this.showDialog = false;
        this.saving = false;
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err?.error?.message || 'Échec de l\'enregistrement' });
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
