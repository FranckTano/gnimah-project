import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ClientService } from '../../../core/services/client.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { SejourService } from '../../../core/services/sejour.service';
import { ClientResponse, ClientRequest } from '../../../core/models/client.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { SejourRequest, SejourResponse } from '../../../core/models/sejour.model';

@Component({
  selector: 'app-check-in',
  standalone: false,
  templateUrl: './check-in.component.html',
  styleUrl: './check-in.component.scss'
})
export class CheckInComponent implements OnInit {
  currentStep = 1;

  // Step 1 – Client
  searchQuery = '';
  searchingClient = false;
  foundClient: ClientResponse | null = null;
  clientNotFound = false;
  showCreateClient = false;
  clientForm!: FormGroup;

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

  // Step 2 – Chambre
  chambresLibres: ChambreResponse[] = [];
  loadingChambres = false;
  selectedChambre: ChambreResponse | null = null;
  sejourForm!: FormGroup;

  typeLocationOptions = [
    { label: 'Passage (heures)', value: 'PASSAGE' },
    { label: 'Séjour (nuitées)', value: 'SEJOUR' }
  ];

  // Step 3 – Paiement
  paiementForm!: FormGroup;
  modePaiementOptions = [
    { label: 'Espèces', value: 'ESPECES' },
    { label: 'Carte bancaire', value: 'CARTE' },
    { label: 'Virement', value: 'VIREMENT' },
    { label: 'Mobile Money', value: 'MOBILE_MONEY' }
  ];

  // Result
  checkingIn = false;
  createdSejour: SejourResponse | null = null;
  success = false;

  constructor(
    private clientService: ClientService,
    private chambreService: ChambreService,
    private sejourService: SejourService,
    private messageService: MessageService,
    private router: Router,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadChambresLibres();
  }

  initForms(): void {
    this.clientForm = this.fb.group({
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

    this.sejourForm = this.fb.group({
      typeLocation: ['PASSAGE', Validators.required],
      dateEntree: [new Date(), Validators.required],
      dateSortie: [null],
      heureEntree: ['', Validators.required],
      heureSortie: [''],
      notes: ['']
    });

    this.paiementForm = this.fb.group({
      montantPaye: [0, [Validators.required, Validators.min(0)]],
      modePaiement: ['ESPECES', Validators.required]
    });

    // Set default time
    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    this.sejourForm.patchValue({ heureEntree: `${hh}:${mm}` });
  }

  loadChambresLibres(): void {
    this.loadingChambres = true;
    this.chambreService.findDisponibles().subscribe({
      next: (data) => {
        this.chambresLibres = data;
        this.loadingChambres = false;
      },
      error: () => {
        this.loadingChambres = false;
      }
    });
  }

  searchClient(): void {
    if (!this.searchQuery.trim()) return;
    this.searchingClient = true;
    this.foundClient = null;
    this.clientNotFound = false;

    const query = this.searchQuery.trim();
    const obs = /^\d/.test(query)
      ? this.clientService.findByTelephone(query)
      : this.clientService.findByPiece(query);

    obs.subscribe({
      next: (client) => {
        this.foundClient = client;
        this.clientNotFound = false;
        this.searchingClient = false;
      },
      error: () => {
        this.foundClient = null;
        this.clientNotFound = true;
        this.searchingClient = false;
      }
    });
  }

  createNewClient(): void {
    if (this.clientForm.invalid) {
      this.clientForm.markAllAsTouched();
      return;
    }
    const request: ClientRequest = this.clientForm.value;
    this.clientService.create(request).subscribe({
      next: (client) => {
        this.foundClient = client;
        this.showCreateClient = false;
        this.messageService.add({ severity: 'success', summary: 'Client créé', detail: client.nomComplet });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Création du client impossible' });
      }
    });
  }

  selectChambre(chambre: ChambreResponse): void {
    this.selectedChambre = chambre;
  }

  goToStep(step: number): void {
    if (step === 2 && !this.foundClient) {
      this.messageService.add({ severity: 'warn', summary: 'Client requis', detail: 'Veuillez sélectionner un client' });
      return;
    }
    if (step === 3 && !this.selectedChambre) {
      this.messageService.add({ severity: 'warn', summary: 'Chambre requise', detail: 'Veuillez sélectionner une chambre' });
      return;
    }
    this.currentStep = step;
  }

  get montantTotal(): number {
    if (!this.selectedChambre) return 0;
    const type = this.sejourForm.get('typeLocation')?.value;
    return type === 'PASSAGE' ? this.selectedChambre.tarifPassage : this.selectedChambre.tarifNuitee;
  }

  get resteAPayer(): number {
    return Math.max(0, this.montantTotal - (this.paiementForm.get('montantPaye')?.value || 0));
  }

  confirmCheckIn(): void {
    if (!this.foundClient || !this.selectedChambre) return;
    this.checkingIn = true;

    const formVal = this.sejourForm.value;
    const dateEntree = formVal.dateEntree instanceof Date
      ? formVal.dateEntree.toISOString().split('T')[0]
      : formVal.dateEntree;
    const dateSortie = formVal.dateSortie instanceof Date
      ? formVal.dateSortie.toISOString().split('T')[0]
      : formVal.dateSortie;

    const request: SejourRequest = {
      clientId: this.foundClient.id,
      chambreId: this.selectedChambre.id,
      typeLocation: formVal.typeLocation,
      dateEntree,
      dateSortie,
      heureEntree: formVal.heureEntree,
      heureSortie: formVal.heureSortie,
      montantPaye: this.paiementForm.get('montantPaye')?.value || 0,
      notes: formVal.notes
    };

    this.sejourService.checkIn(request).subscribe({
      next: (sejour) => {
        this.createdSejour = sejour;
        this.success = true;
        this.checkingIn = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Check-in réussi',
          detail: `N° Reçu : ${sejour.numeroRecu}`
        });
      },
      error: () => {
        this.checkingIn = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Check-in impossible' });
      }
    });
  }

  printReceipt(): void {
    window.print();
  }

  newCheckIn(): void {
    this.currentStep = 1;
    this.foundClient = null;
    this.selectedChambre = null;
    this.clientNotFound = false;
    this.showCreateClient = false;
    this.searchQuery = '';
    this.success = false;
    this.createdSejour = null;
    this.initForms();
    this.loadChambresLibres();
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-CI', {
      style: 'currency', currency: 'XOF', maximumFractionDigits: 0
    }).format(amount || 0);
  }

  isClientFieldInvalid(field: string): boolean {
    const ctrl = this.clientForm.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }
}
