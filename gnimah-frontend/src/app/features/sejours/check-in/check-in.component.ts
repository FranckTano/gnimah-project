import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ClientService } from '../../../core/services/client.service';
import { ChambreService } from '../../../core/services/chambre.service';
import { SejourService } from '../../../core/services/sejour.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { PageHeaderService } from '../../../core/services/page-header.service';
import { MediaService } from '../../../core/services/media.service';
import { ClientResponse, ClientRequest } from '../../../core/models/client.model';
import { ChambreResponse } from '../../../core/models/chambre.model';
import { ReservationResponse } from '../../../core/models/reservation.model';
import { SejourRequest, SejourResponse, TYPE_REMISE_OPTIONS } from '../../../core/models/sejour.model';
import { modePaiementOptions, modePaiementLabel } from '../../../core/models/paiement.model';
import { iso } from '../../../shared/periode';

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
  /** Plusieurs fiches répondent à la recherche : l'agent choisit la bonne. */
  resultatsClient: ClientResponse[] = [];
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

  // Réservation honorée : son acompte vient en déduction de la note.
  reservation: ReservationResponse | null = null;
  /** La chambre réservée n'est pas libre : l'agent doit en attribuer une autre. */
  chambreReserveeIndisponible = false;

  // Result
  checkingIn = false;
  createdSejour: SejourResponse | null = null;
  success = false;

  constructor(
    private clientService: ClientService,
    private chambreService: ChambreService,
    private sejourService: SejourService,
    private reservationService: ReservationService,
    private messageService: MessageService,
    private router: Router,
    private route: ActivatedRoute,
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
    const reservationId = Number(this.route.snapshot.queryParamMap.get('reservation'));
    if (reservationId > 0) {
      this.chargerReservation(reservationId);
    }
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

    // Dates au format « AAAA-MM-JJ » attendu par les champs date, et calculées
    // en heure locale (toISOString basculerait en UTC et pourrait changer le jour).
    this.sejourForm = this.fb.group({
      typeLocation: ['SEJOUR', Validators.required],
      dateEntree: [iso(new Date()), Validators.required],
      dateSortie: [''],
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
      next: (data) => {
        this.chambresLibres = data;
        this.loadingChambres = false;
        this.selectionnerChambreReservee();
      },
      error: () => { this.loadingChambres = false; }
    });
  }

  // ------------------------------------------------------------------
  // Arrivée d'une réservation
  // ------------------------------------------------------------------

  private chargerReservation(id: number): void {
    this.reservationService.getById(id).subscribe({
      next: (r) => {
        if (r.statut === 'ANNULEE' || r.statut === 'ARRIVEE') {
          this.messageService.add({
            severity: 'warn', summary: 'Réservation non utilisable',
            detail: r.statut === 'ANNULEE' ? 'Cette réservation est annulée' : 'Ce client est déjà enregistré'
          });
          return;
        }
        this.reservation = r;
        this.clientService.getById(r.clientId).subscribe({ next: (c) => { this.foundClient = c; } });

        // Arrivée aujourd'hui (le client peut se présenter en avance ou en
        // retard), départ conservé tel que réservé.
        const depart = (r.dateDepart || '').substring(0, 10);
        this.sejourForm.patchValue({
          typeLocation: 'SEJOUR',
          dateSortie: depart > iso(new Date()) ? depart : ''
        });

        // Un tarif négocié à la réservation est repris tel quel.
        const nuits = r.nbNuits && r.nbNuits > 0 ? r.nbNuits : 1;
        const tarifReserve = r.montantBrut ? Math.round(r.montantBrut / nuits) : null;
        this.tarifForm.patchValue({
          tarifPersonnalise: tarifReserve,
          remiseType: r.remiseType,
          remiseValeur: r.remiseValeur > 0 ? r.remiseValeur : null,
          remiseMotif: r.remiseMotif || ''
        });
        this.afficherRemise = !!r.remiseType;
        this.selectionnerChambreReservee();
      },
      error: (err) => this.erreur(err, 'Réservation introuvable')
    });
  }

  private selectionnerChambreReservee(): void {
    const r = this.reservation;
    if (!r || !r.chambreId || this.loadingChambres) return;
    const chambre = this.chambresLibres.find(c => c.id === r.chambreId) || null;
    this.chambreReserveeIndisponible = !chambre;
    if (chambre) this.selectedChambre = chambre;
  }

  /** Acompte déjà encaissé à la réservation, déduit de la note. */
  get acompteReservation(): number {
    return this.reservation ? Number(this.reservation.acompte) || 0 : 0;
  }

  // ------------------------------------------------------------------
  // Client
  // ------------------------------------------------------------------

  get isSejour(): boolean {
    return this.sejourForm.get('typeLocation')?.value === 'SEJOUR';
  }

  setMode(mode: 'SEJOUR' | 'PASSAGE'): void {
    this.sejourForm.patchValue({ typeLocation: mode });
  }

  /**
   * Recherche par nom, téléphone (avec ou sans espaces) ou numéro de pièce.
   * Une seule fiche : elle est retenue ; plusieurs : l'agent choisit.
   */
  searchClient(): void {
    const query = this.searchQuery.trim();
    if (!query) return;
    this.searchingClient = true;
    this.foundClient = null;
    this.clientNotFound = false;
    this.resultatsClient = [];

    this.clientService.search(query, 0, 8).subscribe({
      next: (page) => {
        this.searchingClient = false;
        const clients = page.content || [];
        if (clients.length === 1) {
          this.foundClient = clients[0];
        } else if (clients.length > 1) {
          this.resultatsClient = clients;
        } else {
          this.clientNotFound = true;
        }
      },
      error: () => { this.clientNotFound = true; this.searchingClient = false; }
    });
  }

  choisirClient(c: ClientResponse): void {
    this.foundClient = c;
    this.resultatsClient = [];
  }

  changerClient(): void {
    this.foundClient = null;
    this.searchQuery = '';
    this.clientNotFound = false;
    this.resultatsClient = [];
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
      error: (err) => this.erreur(err, 'Création du client impossible')
    });
  }

  selectChambre(chambre: ChambreResponse): void {
    this.selectedChambre = chambre;
  }

  basculerRemise(): void {
    this.afficherRemise = !this.afficherRemise;
  }

  // ------------------------------------------------------------------
  // Montants
  // ------------------------------------------------------------------

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
    const d1 = new Date(`${entree}T00:00:00`).getTime();
    const d2 = new Date(`${sortie}T00:00:00`).getTime();
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
    const verse = Number(this.paiementForm.get('montantPaye')?.value) || 0;
    return Math.max(0, this.montantTotal - this.acompteReservation - verse);
  }

  // ------------------------------------------------------------------
  // Validation
  // ------------------------------------------------------------------

  confirmCheckIn(): void {
    if (!this.foundClient) {
      this.messageService.add({ severity: 'warn', summary: 'Client requis', detail: 'Recherchez ou créez un client' });
      return;
    }
    if (!this.selectedChambre) {
      this.messageService.add({ severity: 'warn', summary: 'Chambre requise', detail: 'Sélectionnez une chambre disponible' });
      return;
    }
    const formVal = this.sejourForm.value;
    const dateEntree = formVal.dateEntree instanceof Date ? iso(formVal.dateEntree) : formVal.dateEntree;
    const dateSortie = formVal.dateSortie instanceof Date ? iso(formVal.dateSortie) : formVal.dateSortie;
    if (!dateEntree) {
      this.messageService.add({ severity: 'warn', summary: 'Date requise', detail: "Indiquez la date d'entrée" });
      return;
    }
    if (this.isSejour && dateSortie && dateSortie <= dateEntree) {
      this.messageService.add({ severity: 'warn', summary: 'Dates incohérentes', detail: "La date de sortie doit suivre la date d'entrée" });
      return;
    }
    this.checkingIn = true;

    const tarif = this.tarifForm.value;
    const request: SejourRequest = {
      clientId: this.foundClient.id,
      chambreId: this.selectedChambre.id,
      typeLocation: formVal.typeLocation,
      dateEntree,
      dateSortie: this.isSejour && dateSortie ? dateSortie : undefined,
      heureEntree: formVal.heureEntree || undefined,
      heureSortie: formVal.heureSortie || undefined,
      tarifPersonnalise: Number(tarif.tarifPersonnalise) > 0 ? Number(tarif.tarifPersonnalise) : null,
      remiseType: tarif.remiseType || null,
      remiseValeur: Number(tarif.remiseValeur) > 0 ? Number(tarif.remiseValeur) : null,
      remiseMotif: tarif.remiseMotif || null,
      montantPaye: Number(this.paiementForm.get('montantPaye')?.value) || 0,
      modePaiement: this.paiementForm.get('modePaiement')?.value || 'ESPECES',
      notes: formVal.notes,
      reservationId: this.reservation?.id
    };

    this.sejourService.checkIn(request).subscribe({
      next: (sejour) => {
        this.createdSejour = sejour;
        this.success = true;
        this.checkingIn = false;
        this.messageService.add({ severity: 'success', summary: 'Séjour enregistré', detail: `N° Reçu : ${sejour.numeroRecu}` });
      },
      error: (err) => {
        this.checkingIn = false;
        this.erreur(err, 'Enregistrement impossible');
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
    this.resultatsClient = [];
    this.searchQuery = '';
    this.success = false;
    this.createdSejour = null;
    this.afficherRemise = false;
    this.reservation = null;
    this.chambreReserveeIndisponible = false;
    if (this.route.snapshot.queryParamMap.has('reservation')) {
      this.router.navigate([], { queryParams: {} });
    }
    this.initForms();
    this.loadChambresLibres();
  }

  private erreur(err: any, defaut: string): void {
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err?.error?.message || defaut });
  }

  isClientFieldInvalid(field: string): boolean {
    const ctrl = this.clientForm.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }
}
