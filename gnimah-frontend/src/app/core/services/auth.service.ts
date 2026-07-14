import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { CurrentUser, LoginRequest, LoginResponse } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/login`, request).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        const user: CurrentUser = {
          userId: response.userId,
          username: response.username,
          nom: response.nom,
          prenom: response.prenom,
          role: response.role,
          email: response.email,
          nomComplet: `${response.nom} ${response.prenom}`
        };
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  get currentUser(): CurrentUser | null {
    return this.currentUserSubject.value;
  }

  isDirecteur(): boolean {
    return this.currentUser?.role === 'DIRECTEUR';
  }

  isAdmin(): boolean {
    return this.currentUser?.role === 'ADMIN';
  }

  isAgent(): boolean {
    return this.currentUser?.role === 'AGENT';
  }

  hasDirecteurAccess(): boolean {
    return this.isDirecteur() || this.isAdmin();
  }

  private loadUser(): CurrentUser | null {
    const data = localStorage.getItem('currentUser');
    return data ? JSON.parse(data) : null;
  }
}
