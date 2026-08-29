import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ClientService } from '../../../core/services/client.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { SejourService } from '../../../core/services/sejour.service';
import { PageHeaderService } from '../../../core/services/page-header.service';
import { MediaService } from '../../../core/services/media.service';
import { ClientResponse, ClientRequest } from '../../../core/models/client.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { SejourRequest, SejourResponse, TYPE_REMISE_OPTIONS } from '../../../core/models/sejour.model';
import { modePaiementOptions, modePaiementLabel } from '../../../core/models/paiement.model';

@Component({
  selector: 'app-check-in',
  standalone: false,
  templateUrl: './check-in.component.html',
  styleUrl: './check-in.component.scss'
})
export class CheckInComponent implements OnInit {
  // Client
  searchQuery = '';
  searchingClient = false;
  foundClient: ClientResponse | null = null;
  clientNotFound = false;
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

  // Chambre & dates
  chambresLibres: ChambreResponse[] = [];
  loadingChambres = false;
  selectedChambre: ChambreResponse | null = null;
  sejourForm!: FormGroup;

  // Tarification (tarif négocié + remise commerciale)
  tarifForm!: FormGroup;
  afficherRemise = false;
  typeRemiseOptions = TYPE_REMISE_OPTIONS;

  // Paiement
  paiementForm!: FormGroup;
  modePaiementOptions = modePaiementOptions;
  modePaiementLabel = modePaiementLabel;

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
    private fb: FormBuilder,
    private pageHeaderService: PageHeaderService,
    private media: MediaService
  ) {}

  /**
   * Aperçu de la chambre en cours d'attribution : l'agent voit ce qu'il donne au
   * client. Photo téléversée si elle existe, sinon un visuel réel de la résidence.
   */
  get apercuChambre(): string | null {
    if (!this.selectedChambre) return null;
    const televersee = (this.selectedChambre.photos || '')
      .split(',').map(u => u.trim()).find(u => !!u);
    return televersee || this.media.photoChambre(this.selectedChambre.type, this.selectedChambre.numero).thumb;
  }

  ngOnInit(): void {
    this.pageHeaderService.set("Enregistrement d'un séjour", "Enregistrement d'arrivée · émission du reçu");
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
      typeLocation: ['SEJOUR', Validators.required],
      dateEntree: [new Date(), Validators.required],
      dateSortie: [null],
      heureEntree: ['', Validators.required],
      heureSortie: [''],
      notes: ['']
    });

    this.tarifForm = this.fb.group({
      tarifPersonnalise: [null],
      remiseType: [null],
      remiseValeur: [null],
      remiseMotif: ['']
    });

    this.paiementForm = this.fb.group({
      montantPaye: [0, [Validators.required, Validators.min(0)]],
      modePaiement: ['ESPECES', Validators.required]
    });

    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    this.sejourForm.patchValue({ heureEntree: `${hh}:${mm}` });
  }

  loadChambresLibres(): void {
    this.loadingChambres = true;
    this.chambreService.findDisponibles().subscribe({
      next: (data) => { this.chambresLibres = data; this.loadingChambres = false; },
      error: () => { this.loadingChambres = false; }
    });
  }

  get isSejour(): boolean {
    return this.sejourForm.get('typeLocation')?.value === 'SEJOUR';
  }

  setMode(mode: 'SEJOUR' | 'PASSAGE'): void {
    this.sejourForm.patchValue({ typeLocation: mode });
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
      next: (client) => { this.foundClient = client; this.clientNotFound = false; this.searchingClient = false; },
      error: () => { this.foundClient = null; this.clientNotFound = true; this.searchingClient = false; }
    });
  }

  changerClient(): void {
    this.foundClient = null;
    this.searchQuery = '';
    this.clientNotFound = false;
  }

  createNewClient(): void {
    if (this.clientForm.invalid) {
      this.clientForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Champs requis', detail: 'Merci de compléter les champs client obligatoires' });
      return;
    }
    const request: ClientRequest = this.clientForm.value;
    this.clientService.create(request).subscribe({
      next: (client) => {
        this.foundClient = client;
        this.clientNotFound = false;
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

  basculerRemise(): void {
    this.afficherRemise = !this.afficherRemise;
  }

  /** Tarif catalogue de la chambre, affiché en filigrane du champ tarif négocié. */
  get tarifCatalogue(): string {
    if (!this.selectedChambre) return '0';
    const t = this.isSejour ? this.selectedChambre.tarifNuitee : this.selectedChambre.tarifPassage;
    return `${t}`;
  }

  /** Tarif unitaire réellement appliqué : le négocié s'il existe, sinon le catalogue. */
  get tarifApplique(): number {
    if (!this.selectedChambre) return 0;
    const negocie = Number(this.tarifForm?.value?.tarifPersonnalise) || 0;
    if (negocie > 0) return negocie;
    return this.isSejour ? this.selectedChambre.tarifNuitee : this.selectedChambre.tarifPassage;
  }

  /**
   * Nombre d'unités facturées. Un passage est un forfait ; un séjour se compte
   * en nuitées, au minimum une même si les dates coïncident.
   */
  get nbUnites(): number {
    if (!this.isSejour) return 1;
    const entree = this.sejourForm?.get('dateEntree')?.value;
    const sortie = this.sejourForm?.get('dateSortie')?.value;
    if (!entree || !sortie) return 1;
    const d1 = new Date(entree).getTime();
    const d2 = new Date(sortie).getTime();
    return Math.max(1, Math.round((d2 - d1) / 86_400_000));
  }

  get montantBrut(): number {
    return this.tarifApplique * this.nbUnites;
  }

  /**
   * Remise en francs. Le calcul est refait côté serveur, qui fait autorité :
   * ici, il ne sert qu'à annoncer le prix au client avant de valider.
   */
  get remiseMontant(): number {
    const type = this.tarifForm?.value?.remiseType;
    const valeur = Number(this.tarifForm?.value?.remiseValeur) || 0;
    if (!type || valeur <= 0) return 0;
    const remise = type === 'POURCENTAGE'
      ? Math.round(this.montantBrut * Math.min(valeur, 100) / 100)
      : valeur;
    return Math.min(remise, this.montantBrut);
  }

  get montantTotal(): number {
    return this.montantBrut - this.remiseMontant;
  }

  get resteAPayer(): number {
    return Math.max(0, this.montantTotal - (this.paiementForm.get('montantPaye')?.value || 0));
  }

  confirmCheckIn(): void {
    if (!this.foundClient) {
      this.messageService.add({ severity: 'warn', summary: 'Client requis', detail: 'Recherchez ou créez un client' });
      return;
    }
    if (!this.selectedChambre) {
      this.messageService.add({ severity: 'warn', summary: 'Chambre requise', detail: 'Sélectionnez une chambre disponible' });
      return;
    }
    this.checkingIn = true;

    const formVal = this.sejourForm.value;
    const dateEntree = formVal.dateEntree instanceof Date ? formVal.dateEntree.toISOString().split('T')[0] : formVal.dateEntree;
    const dateSortie = formVal.dateSortie instanceof Date ? formVal.dateSortie.toISOString().split('T')[0] : formVal.dateSortie;

    const tarif = this.tarifForm.value;
    const request: SejourRequest = {
      clientId: this.foundClient.id,
      chambreId: this.selectedChambre.id,
      typeLocation: formVal.typeLocation,
      dateEntree,
      dateSortie,
      heureEntree: formVal.heureEntree,
      heureSortie: formVal.heureSortie,
      tarifPersonnalise: Number(tarif.tarifPersonnalise) > 0 ? Number(tarif.tarifPersonnalise) : null,
      remiseType: tarif.remiseType || null,
      remiseValeur: Number(tarif.remiseValeur) > 0 ? Number(tarif.remiseValeur) : null,
      remiseMotif: tarif.remiseMotif || null,
      montantPaye: this.paiementForm.get('montantPaye')?.value || 0,
      modePaiement: this.paiementForm.get('modePaiement')?.value || 'ESPECES',
      notes: formVal.notes
    };

    this.sejourService.checkIn(request).subscribe({
      next: (sejour) => {
        this.createdSejour = sejour;
        this.success = true;
        this.checkingIn = false;
        this.messageService.add({ severity: 'success', summary: 'Séjour enregistré', detail: `N° Reçu : ${sejour.numeroRecu}` });
      },
      error: () => {
        this.checkingIn = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Enregistrement impossible' });
      }
    });
  }

  printReceipt(): void {
    window.print();
  }

  newCheckIn(): void {
    this.foundClient = null;
    this.selectedChambre = null;
    this.clientNotFound = false;
    this.searchQuery = '';
    this.success = false;
    this.createdSejour = null;
    this.afficherRemise = false;
    this.initForms();
    this.loadChambresLibres();
  }

  formatCurrency(amount: number | undefined): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(amount || 0);
  }

  isClientFieldInvalid(field: string): boolean {
    const ctrl = this.clientForm.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }
}
