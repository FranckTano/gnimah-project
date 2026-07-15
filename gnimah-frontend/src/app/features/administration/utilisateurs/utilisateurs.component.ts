import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UtilisateurService } from '../../../core/services/utilisateur.service';
import { UtilisateurResponse, roleLabel } from '../../../core/models/utilisateur.model';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PageHeaderService } from '../../../core/services/page-header.service';

@Component({
  selector: 'app-utilisateurs',
  standalone: false,
  templateUrl: './utilisateurs.component.html',
  styleUrl: './utilisateurs.component.scss'
})
export class UtilisateursComponent implements OnInit {
  utilisateurs: UtilisateurResponse[] = [];
  loading = false;
  showDialog = false;
  editingId: number | null = null;
  saving = false;
  form!: FormGroup;
  roleLabels = roleLabel;
  search = '';
  filtreRole = '';

  roles = [
    { label: 'Administrateur', value: 'ADMIN' },
    { label: 'Directeur', value: 'DIRECTEUR' },
    { label: 'Agent de réception', value: 'AGENT' }
  ];

  constructor(
    private fb: FormBuilder,
    private utilisateurService: UtilisateurService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Personnel', 'Comptes et rôles du personnel de l\'hôtel');
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: [''],
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      role: ['AGENT', Validators.required],
      actif: [true]
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.utilisateurService.findAll().subscribe({
      next: (data) => { this.utilisateurs = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openNew(): void {
    this.editingId = null;
    this.form.reset({ role: 'AGENT', actif: true });
    this.form.get('password')!.setValidators([Validators.required, Validators.minLength(6)]);
    this.form.get('password')!.updateValueAndValidity();
    this.showDialog = true;
  }

  openEdit(u: UtilisateurResponse): void {
    this.editingId = u.id;
    this.form.patchValue({ ...u });
    this.form.get('password')!.clearValidators();
    this.form.get('password')!.updateValueAndValidity();
    this.showDialog = true;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const v = this.form.value;
    const request = { ...v };
    if (!request.password) delete request.password;

    const obs = this.editingId
      ? this.utilisateurService.update(this.editingId, request)
      : this.utilisateurService.create(request);
    obs.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: this.editingId ? 'Utilisateur modifié' : 'Utilisateur créé' });
        this.showDialog = false; this.saving = false; this.load();
      },
      error: (err) => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err?.error?.message || 'Opération échouée' });
      }
    });
  }

  toggleActif(u: UtilisateurResponse): void {
    this.confirmationService.confirm({
      message: `${u.actif ? 'Désactiver' : 'Activer'} l'utilisateur ${u.username} ?`,
      header: 'Confirmation',
      accept: () => {
        this.utilisateurService.toggleActif(u.id).subscribe({
          next: () => { this.load(); },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Opération impossible' })
        });
      }
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f); return !!(c && c.invalid && c.touched);
  }

  roleTone(role: string): string {
    return { ADMIN: 'wine', DIRECTEUR: 'gold', AGENT: 'slate' }[role] || 'gray';
  }

  initials(u: UtilisateurResponse): string {
    return `${u.prenom.charAt(0)}${u.nom.charAt(0)}`.toUpperCase();
  }

  get filtered(): UtilisateurResponse[] {
    const q = this.search.trim().toLowerCase();
    return this.utilisateurs.filter(u => {
      if (this.filtreRole && u.role !== this.filtreRole) return false;
      if (q && !`${u.prenom} ${u.nom} ${u.username} ${u.email}`.toLowerCase().includes(q)) return false;
      return true;
    });
  }
}
