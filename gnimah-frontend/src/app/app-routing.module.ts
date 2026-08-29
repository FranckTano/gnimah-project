import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ClientListComponent } from './features/clients/list/client-list.component';
import { ChambrePlanComponent } from './features/chambres/plan/chambre-plan.component';
import { ChambreListComponent } from './features/chambres/list/chambre-list.component';
import { SejourListComponent } from './features/sejours/list/sejour-list.component';
import { CheckInComponent } from './features/sejours/check-in/check-in.component';
import { ReservationListComponent } from './features/reservations/list/reservation-list.component';
import { ReservationFormComponent } from './features/reservations/form/reservation-form.component';
import { PaiementsComponent } from './features/paiements/paiements.component';
import { EntretienComponent } from './features/entretien/entretien.component';
import { IncidentsComponent } from './features/entretien/incidents/incidents.component';
import { EvenementsComponent } from './features/evenements/evenements.component';
import { KpiComponent } from './features/kpi/kpi.component';
import { ResidenceComponent } from './features/residence/residence.component';
import { ComptabiliteComponent } from './features/comptabilite/comptabilite.component';
import { DepensesComponent } from './features/comptabilite/depenses/depenses.component';
import { CaisseComponent } from './features/caisse/caisse.component';
import { VersementsComponent } from './features/caisse/versements/versements.component';
import { RestaurationComponent } from './features/restauration/restauration.component';
import { StockComponent } from './features/stock/stock.component';
import { UtilisateursComponent } from './features/administration/utilisateurs/utilisateurs.component';
import { MediasComponent } from './features/administration/medias/medias.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'residence', component: ResidenceComponent },
      { path: 'clients', component: ClientListComponent },
      { path: 'chambres', component: ChambrePlanComponent },
      { path: 'chambres/liste', component: ChambreListComponent },
      { path: 'sejours', component: SejourListComponent },
      { path: 'sejours/check-in', component: CheckInComponent },
      { path: 'reservations', component: ReservationListComponent },
      { path: 'reservations/new', component: ReservationFormComponent },
      { path: 'paiements', component: PaiementsComponent },
      { path: 'entretien', component: EntretienComponent },
      // Les incidents sont déclarés par l'équipe d'entretien elle-même : la page
      // reste ouverte à tous les agents, seule la résolution est restreinte.
      { path: 'entretien/incidents', component: IncidentsComponent },
      { path: 'evenements', component: EvenementsComponent },
      { path: 'restauration', component: RestaurationComponent },
      { path: 'stock', component: StockComponent },
      // La caisse est l'outil de travail des agents de réception : pas de
      // restriction de rôle, le contrôle se fait par la traçabilité des
      // mouvements et le comptage de fermeture.
      { path: 'caisse', component: CaisseComponent },
      { path: 'caisse/versements', component: VersementsComponent },
      // La comptabilité expose la marge et le détail des charges : direction seule.
      { path: 'comptabilite', component: ComptabiliteComponent, canActivate: [AuthGuard], data: { role: 'DIRECTEUR' } },
      { path: 'comptabilite/depenses', component: DepensesComponent },
      { path: 'kpi', component: KpiComponent, canActivate: [AuthGuard], data: { role: 'DIRECTEUR' } },
      { path: 'administration/utilisateurs', component: UtilisateursComponent, canActivate: [AuthGuard], data: { role: 'ADMIN' } },
      // La galerie est un contenu éditorial, pas de la gestion de comptes : le
      // directeur y a accès au même titre que l'administrateur.
      { path: 'administration/medias', component: MediasComponent, canActivate: [AuthGuard], data: { role: 'DIRECTEUR' } }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
