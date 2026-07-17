import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { PasswordModule } from 'primeng/password';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { PublicInfoService } from '../../../core/services/public-info.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let publicInfoServiceSpy: jasmine.SpyObj<PublicInfoService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'login', 'forgotPassword']);
    authServiceSpy.isLoggedIn.and.returnValue(false);
    publicInfoServiceSpy = jasmine.createSpyObj('PublicInfoService', ['getHotelInfo']);
    publicInfoServiceSpy.getHotelInfo.and.returnValue(of({ totalChambres: 12 }));
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [ReactiveFormsModule, NoopAnimationsModule, PasswordModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PublicInfoService, useValue: publicInfoServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MessageService, useValue: messageServiceSpy }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('redirects to /dashboard immediately if already logged in', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    const f = TestBed.createComponent(LoginComponent);

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('loads the public hotel info on init', () => {
    expect(component.hotelInfo?.totalChambres).toBe(12);
  });

  it('does not submit an invalid (empty) login form', () => {
    component.form.setValue({ username: '', password: '' });

    component.onSubmit();

    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('logs in and navigates to /dashboard on valid submit', () => {
    authServiceSpy.login.and.returnValue(of({
      token: 't', type: 'Bearer', userId: 1, username: 'agent', nom: 'A', prenom: 'B', role: 'AGENT', email: 'a@b.com'
    }));
    component.form.setValue({ username: 'agent', password: 'Agent@2026' });

    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({ username: 'agent', password: 'Agent@2026' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('shows an error toast and stops loading when login fails', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ error: { message: 'Identifiants invalides' } })));
    component.form.setValue({ username: 'agent', password: 'wrong' });

    component.onSubmit();

    expect(component.loading).toBeFalse();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
    expect(routerSpy.navigate).not.toHaveBeenCalledWith(['/dashboard']);
  });

  it('opens the forgot-password dialog with a reset form', () => {
    component.forgotForm.setValue({ username: 'leftover' });

    component.openForgotDialog();

    expect(component.showForgotDialog).toBeTrue();
    expect(component.forgotForm.value.username).toBeFalsy();
  });

  it('does not call forgotPassword for an invalid (empty) forgot-password form', () => {
    component.forgotForm.setValue({ username: '' });

    component.submitForgot();

    expect(authServiceSpy.forgotPassword).not.toHaveBeenCalled();
  });

  it('submits the forgot-password request and closes the dialog on success', () => {
    authServiceSpy.forgotPassword.and.returnValue(of({ message: 'Demande envoyée à l\'administrateur.' }));
    component.showForgotDialog = true;
    component.forgotForm.setValue({ username: 'agent' });

    component.submitForgot();

    expect(authServiceSpy.forgotPassword).toHaveBeenCalledWith({ username: 'agent' });
    expect(component.showForgotDialog).toBeFalse();
    expect(component.forgotSending).toBeFalse();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
  });
});
