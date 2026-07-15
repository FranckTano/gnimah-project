import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CurrentUser } from '../../../core/models/auth.model';
import { PageHeaderService } from '../../../core/services/page-header.service';

interface NavItem {
  label: string;
  icon: string;
  link: string;
}

interface NavSection {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'app-layout',
  standalone: false,
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnInit, OnDestroy {
  drawerOpen = false;
  currentUser: CurrentUser | null = null;
  navSections: NavSection[] = [];
  pageTitle = '';
  pageSub = '';
  today = new Date();

  private sub = new Subscription();

  constructor(private authService: AuthService, private pageHeaderService: PageHeaderService) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    this.buildMenu();
    this.sub.add(
      this.pageHeaderService.header$.subscribe(h => {
        this.pageTitle = h.title;
        this.pageSub = h.subtitle;
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  buildMenu(): void {
    const isDirecteur = this.authService.hasDirecteurAccess();
    const isAdmin = this.authService.isAdmin();

    const general: NavItem[] = [
      { label: 'Tableau de bord', icon: 'ti-layout-dashboard', link: '/dashboard' }
    ];
    if (isDirecteur) {
      general.push({ label: 'Statistiques & KPI', icon: 'ti-chart-line', link: '/kpi' });
    }

    const operations: NavItem[] = [
      { label: 'Check-in / Séjour', icon: 'ti-login-2', link: '/sejours/check-in' },
      { label: 'Séjours en cours', icon: 'ti-bed', link: '/sejours' },
      { label: 'Réservations', icon: 'ti-calendar-check', link: '/reservations' },
      { label: 'Plan des chambres', icon: 'ti-layout-grid', link: '/chambres' },
      { label: 'Housekeeping', icon: 'ti-spray', link: '/entretien' },
      { label: 'Calendrier & événements', icon: 'ti-calendar-event', link: '/evenements' },
      { label: 'Fiches clients', icon: 'ti-users', link: '/clients' }
    ];

    const facturation: NavItem[] = [
      { label: 'Reçus & Caisse', icon: 'ti-receipt-2', link: '/paiements' }
    ];

    this.navSections = [
      { label: 'GÉNÉRAL', items: general },
      { label: 'OPÉRATIONS', items: operations },
      { label: 'FACTURATION', items: facturation }
    ];

    if (isAdmin) {
      this.navSections.push({
        label: 'ADMINISTRATION',
        items: [{ label: 'Utilisateurs', icon: 'ti-shield-lock', link: '/administration/utilisateurs' }]
      });
    }
  }

  get userInitials(): string {
    if (!this.currentUser) return '?';
    const n = (this.currentUser.nom || '').charAt(0);
    const p = (this.currentUser.prenom || '').charAt(0);
    return (n + p).toUpperCase() || '?';
  }

  get roleLabel(): string {
    const role = this.currentUser?.role;
    if (role === 'ADMIN') return 'Administrateur';
    if (role === 'DIRECTEUR') return 'Directeur';
    return 'Agent de réception';
  }

  toggleDrawer(): void {
    this.drawerOpen = !this.drawerOpen;
  }

  closeDrawer(): void {
    this.drawerOpen = false;
  }

  logout(): void {
    this.authService.logout();
  }
}
