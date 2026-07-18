import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CurrentUser } from '../../../core/models/auth.model';
import { PageHeaderService } from '../../../core/services/page-header.service';
import { NotificationService } from '../../../core/services/notification.service';
import { NotificationResponse, NOTIFICATION_ICONS } from '../../../core/models/notification.model';

interface NavItem {
  label: string;
  icon: string;
  link: string;
  exact?: boolean;
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

  showNotifPanel = false;
  unreadCount = 0;
  notifications: NotificationResponse[] = [];
  notifIcons = NOTIFICATION_ICONS;

  private sub = new Subscription();

  constructor(
    private authService: AuthService,
    private pageHeaderService: PageHeaderService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    this.buildMenu();
    this.sub.add(
      this.pageHeaderService.header$.subscribe(h => {
        this.pageTitle = h.title;
        this.pageSub = h.subtitle;
      })
    );
    this.refreshUnreadCount();
    this.sub.add(interval(60000).subscribe(() => this.refreshUnreadCount()));
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  refreshUnreadCount(): void {
    this.notificationService.countNonLues().subscribe({
      next: (res) => { this.unreadCount = res.count; },
      error: () => { /* la cloche reste silencieuse si l'appel échoue */ }
    });
  }

  toggleNotifPanel(): void {
    this.showNotifPanel = !this.showNotifPanel;
    if (this.showNotifPanel) {
      this.notificationService.findRecentes().subscribe({
        next: (data) => { this.notifications = data; },
        error: () => { /* panneau reste vide si l'appel échoue */ }
      });
    }
  }

  onNotifClick(n: NotificationResponse): void {
    if (!n.lu) {
      this.notificationService.marquerLue(n.id).subscribe(() => {
        n.lu = true;
        this.refreshUnreadCount();
      });
    }
    this.showNotifPanel = false;
    if (n.lien) {
      this.router.navigateByUrl(n.lien);
    }
  }

  marquerToutesLues(): void {
    this.notificationService.marquerToutesLues().subscribe(() => {
      this.notifications.forEach(n => n.lu = true);
      this.refreshUnreadCount();
    });
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
      { label: 'Enregistrement', icon: 'ti-login-2', link: '/sejours/check-in' },
      { label: 'Séjours en cours', icon: 'ti-bed', link: '/sejours', exact: true },
      { label: 'Réservations', icon: 'ti-calendar-check', link: '/reservations' },
      { label: 'Plan des chambres', icon: 'ti-layout-grid', link: '/chambres', exact: true },
      { label: 'Chambres', icon: 'ti-door', link: '/chambres/liste' },
      { label: 'Entretien', icon: 'ti-spray', link: '/entretien' },
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
    if (role === 'RESPONSABLE') return 'Responsable';
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
