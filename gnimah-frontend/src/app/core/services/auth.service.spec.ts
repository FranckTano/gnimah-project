import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { LoginResponse } from '../models/auth.model';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const loginResponse: LoginResponse = {
    token: 'signed.jwt.token',
    type: 'Bearer',
    userId: 1,
    username: 'agent',
    nom: 'Réception',
    prenom: 'Agent',
    role: 'AGENT',
    email: 'agent@gnimah.com'
  };

  beforeEach(() => {
    localStorage.clear();
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: Router, useValue: routerSpy }]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts logged out when localStorage is empty', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.currentUser).toBeNull();
  });

  it('login() stores the token and current user, and updates currentUser$', () => {
    service.login({ username: 'agent', password: 'Agent@2026' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(loginResponse);

    expect(localStorage.getItem('token')).toBe('signed.jwt.token');
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.currentUser?.username).toBe('agent');
    expect(service.currentUser?.nomComplet).toBe('Réception Agent');
  });

  it('logout() clears storage, resets currentUser and redirects to /login', () => {
    service.login({ username: 'agent', password: 'Agent@2026' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush(loginResponse);

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('currentUser')).toBeNull();
    expect(service.currentUser).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  describe('role helpers', () => {
    function loginAs(role: 'ADMIN' | 'DIRECTEUR' | 'RESPONSABLE' | 'AGENT'): void {
      service.login({ username: 'u', password: 'p' }).subscribe();
      httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({ ...loginResponse, role });
    }

    it('isAdmin() is true only for ADMIN', () => {
      loginAs('ADMIN');
      expect(service.isAdmin()).toBeTrue();
      expect(service.isDirecteur()).toBeFalse();
    });

    it('hasDirecteurAccess() is true for DIRECTEUR and ADMIN, false otherwise', () => {
      loginAs('DIRECTEUR');
      expect(service.hasDirecteurAccess()).toBeTrue();
    });

    it('hasTaskManagementAccess() is true for RESPONSABLE but not AGENT', () => {
      loginAs('RESPONSABLE');
      expect(service.hasTaskManagementAccess()).toBeTrue();

      loginAs('AGENT');
      expect(service.hasTaskManagementAccess()).toBeFalse();
    });
  });
});
