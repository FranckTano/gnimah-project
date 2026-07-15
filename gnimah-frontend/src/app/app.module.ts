import { NgModule, LOCALE_ID } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

registerLocaleData(localeFr, 'fr');
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

// PrimeNG modules
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuModule } from 'primeng/menu';
import { providePrimeNG } from 'primeng/config';
import Lara from '@primeng/themes/lara';
import { definePreset } from '@primeng/themes';

// GNIMAH brand preset — retints Lara's primary palette to the hotel's wine/burgundy
// identity so every remaining PrimeNG default (dialogs, selects, datepicker, focus rings)
// blends with the hand-styled .gn-* pages instead of showing PrimeNG's stock blue.
const GnimahPreset = definePreset(Lara, {
  semantic: {
    primary: {
      50: '#fbeef1', 100: '#f6dfe4', 200: '#e6b9c5', 300: '#d6939f',
      400: '#bc5b73', 500: '#8c2f4d', 600: '#7a2843', 700: '#671f38',
      800: '#551a2e', 900: '#3f1422', 950: '#2a0d17'
    }
  }
});
import { BadgeModule } from 'primeng/badge';
import { TagModule } from 'primeng/tag';
import { ChipModule } from 'primeng/chip';
import { ProgressBarModule } from 'primeng/progressbar';
import { ChartModule } from 'primeng/chart';
import { ToolbarModule } from 'primeng/toolbar';
import { InputNumberModule } from 'primeng/inputnumber';
import { TextareaModule } from 'primeng/textarea';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TabsModule } from 'primeng/tabs';
import { DataViewModule } from 'primeng/dataview';
import { PanelModule } from 'primeng/panel';
import { DividerModule } from 'primeng/divider';
import { AvatarModule } from 'primeng/avatar';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { MessageModule } from 'primeng/message';
import { ConfirmationService, MessageService } from 'primeng/api';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';

// Feature components
import { LoginComponent } from './features/auth/login/login.component';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ClientListComponent } from './features/clients/list/client-list.component';
import { ClientFormComponent } from './features/clients/form/client-form.component';
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

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    LayoutComponent,
    DashboardComponent,
    ClientListComponent,
    ClientFormComponent,
    ChambrePlanComponent,
    ChambreListComponent,
    SejourListComponent,
    CheckInComponent,
    ReservationListComponent,
    ReservationFormComponent,
    PaiementsComponent,
    EntretienComponent,
    EvenementsComponent,
    KpiComponent,
    UtilisateursComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    CardModule,
    TableModule,
    DialogModule,
    ToastModule,
    ConfirmDialogModule,
    MenuModule,
    BadgeModule,
    TagModule,
    ChipModule,
    ProgressBarModule,
    ChartModule,
    ToolbarModule,
    InputNumberModule,
    TextareaModule,
    SelectButtonModule,
    TabsModule,
    DataViewModule,
    PanelModule,
    DividerModule,
    AvatarModule,
    SkeletonModule,
    TooltipModule,
    MessageModule,
    SelectModule,
    DatePickerModule
  ],
  providers: [
    { provide: LOCALE_ID, useValue: 'fr' },
    MessageService,
    ConfirmationService,
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    providePrimeNG({ theme: { preset: GnimahPreset } })
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
