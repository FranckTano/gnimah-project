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
import { EvenementsComponent } from './features/evenements/evenements.component';
import { KpiComponent } from './features/kpi/kpi.component';
import { UtilisateursComponent } from './features/administration/utilisateurs/utilisateurs.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'clients', component: ClientListComponent },
      { path: 'chambres', component: ChambrePlanComponent },
      { path: 'chambres/liste', component: ChambreListComponent },
      { path: 'sejours', component: SejourListComponent },
      { path: 'sejours/check-in', component: CheckInComponent },
      { path: 'reservations', component: ReservationListComponent },
      { path: 'reservations/new', component: ReservationFormComponent },
      { path: 'paiements', component: PaiementsComponent },
      { path: 'entretien', component: EntretienComponent },
      { path: 'evenements', component: EvenementsComponent },
      { path: 'kpi', component: KpiComponent },
      { path: 'administration/utilisateurs', component: UtilisateursComponent }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
