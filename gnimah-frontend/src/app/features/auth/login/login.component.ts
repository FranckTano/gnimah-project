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

  /**
   * Traduit la réponse du serveur en message actionnable.
   *
   * Le backend répond volontairement « Identifiants invalides » sans préciser
   * si c'est l'identifiant ou le mot de passe : le distinguer confirmerait
   * quels comptes existent. On garde ce principe, en le formulant clairement.
   */
  private messageErreur(err: any): { titre: string; detail: string; icone: string } {
    const statut = err?.status;

    if (statut === 0) {
      return {
        titre: 'Serveur injoignable',
        detail: "Vérifiez votre connexion internet, puis réessayez.",
        icone: 'ti-wifi-off'
      };
    }
    if (statut === 401) {
      return {
        titre: 'Nom d’utilisateur ou mot de passe incorrect',
        detail: "Vérifiez votre saisie. Attention aux majuscules et aux espaces.",
        icone: 'ti-lock-exclamation'
      };
    }
    if (statut === 403) {
      return {
        titre: 'Accès refusé',
        detail: "Ce compte n'est pas autorisé à se connecter. Contactez un administrateur.",
        icone: 'ti-shield-x'
      };
    }
    if (statut === 429) {
      // Le RateLimitFilter bloque après 10 tentatives sur 5 minutes.
      return {
        titre: 'Trop de tentatives',
        detail: err?.error?.message || 'Patientez quelques minutes avant de réessayer.',
        icone: 'ti-clock-exclamation'
      };
    }
    if (statut === 400 && err?.error?.errors) {
      // Le serveur renvoie ses erreurs par champ : on les pose sur les champs.
      const champs = err.error.errors as Record<string, string>;
      Object.entries(champs).forEach(([nom, message]) => {
        this.form.get(nom)?.setErrors({ serveur: message });
      });
      return {
        titre: 'Formulaire incomplet',
        detail: 'Corrigez les champs signalés ci-dessus.',
        icone: 'ti-alert-circle'
      };
    }
    if (statut >= 500) {
      return {
        titre: 'Le serveur a rencontré un problème',
        detail: 'Réessayez dans un instant. Si cela persiste, prévenez un administrateur.',
        icone: 'ti-server-off'
      };
    }
    return {
      titre: 'Connexion impossible',
      detail: err?.error?.message || 'Une erreur inattendue est survenue.',
      icone: 'ti-alert-circle'
    };
  }

  /** Toute frappe efface l'erreur : elle ne concerne plus la saisie en cours. */
  effacerErreur(): void {
    if (this.erreur || this.champsEnCause) {
      this.erreur = null;
      this.champsEnCause = false;
    }
  }

  /** Message à afficher sous un champ, ou null s'il n'y a rien à signaler. */
  erreurChamp(nom: 'username' | 'password'): string | null {
    const c = this.form.get(nom);
    if (!c || !c.errors || !(c.dirty || c.touched)) return null;
    if (c.errors['serveur']) return c.errors['serveur'];
    if (c.errors['required']) {
      return nom === 'username' ? 'Saisissez votre identifiant.' : 'Saisissez votre mot de passe.';
    }
    return null;
  }

  /**
   * Erreur d'authentification affichée sous le formulaire. Un bandeau à cet
   * endroit se lit sans quitter les champs des yeux, là où une notification
   * flottante disparaît avant d'être lue et laisse l'utilisateur sans indice.
   */
  erreur: { titre: string; detail: string; icone: string } | null = null;

  /** Passe les deux champs en rouge : le serveur ne dit pas lequel est en cause. */
  champsEnCause = false;

  onSubmit(): void {
    // Marque les champs pour que leurs messages « obligatoire » apparaissent
    // au lieu d'un formulaire qui ne réagit pas au clic.
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    this.erreur = null;
    this.champsEnCause = false;

    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.erreur = this.messageErreur(err);
        this.champsEnCause = err?.status === 401;
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
