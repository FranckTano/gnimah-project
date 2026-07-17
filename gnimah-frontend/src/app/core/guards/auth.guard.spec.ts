import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'hasDirecteurAccess', 'isAdmin']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(AuthGuard);
  });

  function routeWithRole(role?: string): ActivatedRouteSnapshot {
    return { data: role ? { role } : {} } as unknown as ActivatedRouteSnapshot;
  }

  const state = {} as RouterStateSnapshot;

  it('redirects to /login when not authenticated', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);

    const result = guard.canActivate(routeWithRole(), state);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], jasmine.any(Object));
  });

  it('allows access to a route with no role requirement once logged in', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);

    const result = guard.canActivate(routeWithRole(), state);

    expect(result).toBeTrue();
  });

  it('blocks a DIRECTEUR-only route for a user without directeur access', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.hasDirecteurAccess.and.returnValue(false);

    const result = guard.canActivate(routeWithRole('DIRECTEUR'), state);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('allows a DIRECTEUR-only route for a user with directeur access', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.hasDirecteurAccess.and.returnValue(true);

    const result = guard.canActivate(routeWithRole('DIRECTEUR'), state);

    expect(result).toBeTrue();
  });

  it('blocks an ADMIN-only route for a non-admin', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);

    const result = guard.canActivate(routeWithRole('ADMIN'), state);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('allows an ADMIN-only route for an admin', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(true);

    const result = guard.canActivate(routeWithRole('ADMIN'), state);

    expect(result).toBeTrue();
  });
});
