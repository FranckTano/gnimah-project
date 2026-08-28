import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../core/services/auth.service';
import { PublicInfoService } from '../../../core/services/public-info.service';
import { PublicHotelInfo } from '../../../core/models/public-info.model';
import { MediaService, HotelPhoto } from '../../../core/services/media.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit, OnDestroy {
  form: FormGroup;
  loading = false;
  hotelInfo: PublicHotelInfo | null = null;

  showForgotDialog = false;
  forgotForm: FormGroup;
  forgotSending = false;

  /** Diaporama lent des vraies photos de la résidence, derrière le panneau d'accueil. */
  photos: HotelPhoto[] = [];
  photoActive = 0;
  private rotation?: ReturnType<typeof setInterval>;

  /**
   * Ne met dans le DOM que la photo affichée et la suivante. L'écran de
   * connexion ne télécharge donc que ~2 images au lieu des 5 : la suivante a
   * tout le palier de 7 s pour arriver, le fondu reste net, et la page s'ouvre
   * vite même sur une connexion lente.
   */
  photoChargeable(i: number): boolean {
    return i <= this.photoActive + 1;
  }

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private publicInfoService: PublicInfoService,
    private media: MediaService,
    private router: Router,
    private messageService: MessageService
  ) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
    this.forgotForm = this.fb.group({
      username: ['', Validators.required]
    });
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  ngOnInit(): void {
    this.photos = this.media.diaporamaConnexion;
    // Rotation lente : la photo reste assez longtemps pour être regardée, jamais
    // assez pour distraire quelqu'un qui tape son mot de passe.
    this.rotation = setInterval(() => {
      this.photoActive = (this.photoActive + 1) % this.photos.length;
    }, 7000);

    this.publicInfoService.getHotelInfo().subscribe({
      next: (info) => { this.hotelInfo = info; },
      error: () => { /* la page de connexion reste utilisable sans ces stats */ }
    });
  }

  ngOnDestroy(): void {
    if (this.rotation) clearInterval(this.rotation);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message || 'Identifiants invalides';
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: msg });
      }
    });
  }

  openForgotDialog(): void {
    this.forgotForm.reset();
    this.showForgotDialog = true;
  }

  submitForgot(): void {
    if (this.forgotForm.invalid) return;
    this.forgotSending = true;
    this.authService.forgotPassword(this.forgotForm.value).subscribe({
      next: (res) => {
        this.forgotSending = false;
        this.showForgotDialog = false;
        this.messageService.add({ severity: 'success', summary: 'Demande envoyée', detail: res.message, life: 8000 });
      },
      error: () => {
        this.forgotSending = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'envoyer la demande, réessayez.' });
      }
    });
  }
}
