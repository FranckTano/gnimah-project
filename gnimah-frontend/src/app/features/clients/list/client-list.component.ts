import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ClientService } from '../../../core/services/client.service';
import { ClientRequest, ClientResponse } from '../../../core/models/client.model';
import { PageHeaderService } from '../../../core/services/page-header.service';

@Component({
  selector: 'app-client-list',
  standalone: false,
  templateUrl: './client-list.component.html',
  styleUrl: './client-list.component.scss'
})
export class ClientListComponent implements OnInit {
  clients: ClientResponse[] = [];
  totalRecords = 0;
  loading = false;
  searchTerm = '';
  currentPage = 0;
  pageSize = 20;

  // Dialog
  dialogVisible = false;
  dialogTitle = 'Nouveau client';
  editingId: number | null = null;
  saving = false;

  form!: FormGroup;

  civiliteOptions = [
    { label: 'M.', value: 'M' },
    { label: 'Mme', value: 'Mme' },
    { label: 'Mlle', value: 'Mlle' }
  ];

  typePieceOptions = [
    { label: 'CNI', value: 'CNI' },
    { label: 'Passeport', value: 'PASSEPORT' },
    { label: 'Titre de séjour', value: 'SEJOUR' },
    { label: 'Permis de conduire', value: 'PERMIS' }
  ];

  constructor(
    private clientService: ClientService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private router: Router,
    private fb: FormBuilder,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Fiches clients', 'Base clients et historique des séjours');
    this.initForm();
    this.loadClients();
  }

  initials(client: ClientResponse): string {
    return (client.nomComplet || '?').split(' ').filter(Boolean).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  sinceLabel(client: ClientResponse): string {
    if (!client.createdAt) return '';
    const year = new Date(client.createdAt).getFullYear();
    const thisYear = new Date().getFullYear();
    return year === thisYear ? 'Nouveau' : `Client depuis ${year}`;
  }

  initForm(): void {
    this.form = this.fb.group({
      civilite: ['M', Validators.required],
      nom: ['', Validators.required],
      prenom: [''],
      telephone: ['', Validators.required],
      email: [''],
      typePiece: ['CNI', Validators.required],
      numeroPiece: ['', Validators.required],
      nationalite: [''],
      adresse: ['']
    });
  }

  loadClients(): void {
    this.loading = true;
    this.clientService.search(this.searchTerm || undefined, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.clients = page.content;
        this.totalRecords = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les clients' });
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadClients();
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    this.loadClients();
  }

  openNew(): void {
    this.editingId = null;
    this.dialogTitle = 'Nouveau client';
    this.form.reset({ civilite: 'M', typePiece: 'CNI' });
    this.dialogVisible = true;
  }

  openEdit(client: ClientResponse): void {
    this.editingId = client.id;
    this.dialogTitle = 'Modifier le client';
    this.form.patchValue({
      civilite: client.civilite,
      nom: client.nom,
      prenom: client.prenom,
      telephone: client.telephone,
      email: client.email,
      typePiece: client.typePiece,
      numeroPiece: client.numeroPiece,
      nationalite: client.nationalite,
      adresse: client.adresse
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const request: ClientRequest = this.form.value;

    const obs = this.editingId
      ? this.clientService.update(this.editingId, request)
      : this.clientService.create(request);

    obs.subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: this.editingId ? 'Client modifié' : 'Client créé'
        });
        this.dialogVisible = false;
        this.saving = false;
        this.loadClients();
      },
      error: () => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Opération échouée' });
      }
    });
  }

  confirmDelete(client: ClientResponse): void {
    this.confirmationService.confirm({
      message: `Supprimer le client <strong>${client.nomComplet}</strong> ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.clientService.delete(client.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Supprimé', detail: 'Client supprimé' });
            this.loadClients();
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Suppression impossible' });
          }
        });
      }
    });
  }

  isFieldInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }
}
