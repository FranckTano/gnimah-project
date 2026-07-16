import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../core/services/auth.service';
import { PublicInfoService } from '../../../core/services/public-info.service';
import { PublicHotelInfo } from '../../../core/models/public-info.model';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loading = false;
  hotelInfo: PublicHotelInfo | null = null;

  showForgotDialog = false;
  forgotForm: FormGroup;
  forgotSending = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private publicInfoService: PublicInfoService,
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
    this.publicInfoService.getHotelInfo().subscribe({
      next: (info) => { this.hotelInfo = info; },
      error: () => { /* la page de connexion reste utilisable sans ces stats */ }
    });
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
