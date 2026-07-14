import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AuthService } from '../../../core/services/auth.service';
import { CurrentUser } from '../../../core/models/auth.model';

@Component({
  selector: 'app-layout',
  standalone: false,
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnInit {
  sidebarVisible = true;
  currentUser: CurrentUser | null = null;
  menuItems: MenuItem[] = [];
  userMenuItems: MenuItem[] = [];
  today = new Date();

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    this.buildMenu();
    this.userMenuItems = [
      { label: 'Mon profil', icon: 'pi pi-user' },
      { label: 'Changer mot de passe', icon: 'pi pi-lock', routerLink: '/profil' },
      { separator: true },
      { label: 'Déconnexion', icon: 'pi pi-sign-out', command: () => this.logout() }
    ];
  }

  buildMenu(): void {
    const isDirecteur = this.authService.hasDirecteurAccess();
    const isAdmin = this.authService.isAdmin();

    this.menuItems = [
      {
        label: 'Tableau de bord',
        icon: 'pi pi-home',
        routerLink: '/dashboard'
      },
      {
        label: 'Réception',
        icon: 'pi pi-inbox',
        items: [
          { label: 'Check-in', icon: 'pi pi-sign-in', routerLink: '/sejours/check-in' },
          { label: 'Séjours en cours', icon: 'pi pi-list', routerLink: '/sejours' },
          { label: 'Réservations', icon: 'pi pi-calendar', routerLink: '/reservations' },
        ]
      },
      {
        label: 'Clients',
        icon: 'pi pi-users',
        routerLink: '/clients'
      },
      {
        label: 'Chambres',
        icon: 'pi pi-building',
        items: [
          { label: 'Plan des chambres', icon: 'pi pi-th-large', routerLink: '/chambres' },
          { label: 'Liste', icon: 'pi pi-list', routerLink: '/chambres/liste' },
        ]
      },
      {
        label: 'Caisse',
        icon: 'pi pi-wallet',
        routerLink: '/paiements'
      },
      {
        label: 'Entretien',
        icon: 'pi pi-cog',
        routerLink: '/entretien'
      },
      {
        label: 'Événements',
        icon: 'pi pi-calendar-plus',
        routerLink: '/evenements'
      },
      ...(isDirecteur ? [{
        label: 'Statistiques & KPI',
        icon: 'pi pi-chart-bar',
        routerLink: '/kpi'
      }] : []),
      ...(isAdmin ? [{
        label: 'Administration',
        icon: 'pi pi-shield',
        items: [
          { label: 'Utilisateurs', icon: 'pi pi-users', routerLink: '/administration/utilisateurs' }
        ]
      }] : [])
    ];
  }

  getRoleClass(): string {
    const role = this.currentUser?.role;
    if (role === 'ADMIN') return 'p-tag-danger';
    if (role === 'DIRECTEUR') return 'p-tag-warning';
    return 'p-tag-info';
  }

  logout(): void {
    this.authService.logout();
  }

  toggleSidebar(): void {
    this.sidebarVisible = !this.sidebarVisible;
  }
}
