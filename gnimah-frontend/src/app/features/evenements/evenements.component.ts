import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EvenementService } from '../../core/services/evenement.service';
import { EvenementResponse } from '../../core/models/evenement.model';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PageHeaderService } from '../../core/services/page-header.service';

interface CalCell {
  day: string;
  isToday: boolean;
  hasEv: boolean;
  evLabel: string;
}

interface UpcomingEvent {
  day: string;
  mon: string;
  title: string;
  sub: string;
  time: string;
}

const EVENT_TONES = ['#8c2f4d', '#5a2035', '#2f9e6f', '#c79a35', '#5f7191'];

@Component({
  selector: 'app-evenements',
  standalone: false,
  templateUrl: './evenements.component.html',
  styleUrl: './evenements.component.scss'
})
export class EvenementsComponent implements OnInit {
  evenements: EvenementResponse[] = [];
  loading = false;
  showDialog = false;
  editingId: number | null = null;
  saving = false;
  form!: FormGroup;

  viewDate = new Date();
  weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  constructor(
    private fb: FormBuilder,
    private evenementService: EvenementService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private pageHeaderService: PageHeaderService
  ) {}

  ngOnInit(): void {
    this.pageHeaderService.set('Calendrier & événements', "Occupation et événements de l'hôtel");
    this.form = this.fb.group({
      titre: ['', Validators.required],
      description: [''],
      dateDebut: [null, Validators.required],
      dateFin: [null, Validators.required],
      lieu: ['', Validators.required],
      nombreParticipants: [0],
      montant: [0]
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.evenementService.findByMois(this.viewDate.getFullYear(), this.viewDate.getMonth() + 1).subscribe({
      next: (data) => { this.evenements = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get monthLabel(): string {
    const label = this.viewDate.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  prevMonth(): void {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() - 1, 1);
    this.load();
  }

  nextMonth(): void {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + 1, 1);
    this.load();
  }

  get calCells(): CalCell[] {
    const year = this.viewDate.getFullYear();
    const month = this.viewDate.getMonth();
    const firstDay = new Date(year, month, 1).getDay(); // 0=Sun
    const lead = (firstDay + 6) % 7; // Monday-start offset
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today = new Date();
    const isCurrentMonth = today.getFullYear() === year && today.getMonth() === month;

    const eventsByDay = new Map<number, EvenementResponse[]>();
    this.evenements.forEach(e => {
      const d = new Date(e.dateDebut);
      if (d.getFullYear() === year && d.getMonth() === month) {
        const list = eventsByDay.get(d.getDate()) || [];
        list.push(e);
        eventsByDay.set(d.getDate(), list);
      }
    });

    const totalCells = Math.ceil((lead + daysInMonth) / 7) * 7;
    const cells: CalCell[] = [];
    for (let i = 0; i < totalCells; i++) {
      const dayNum = i - lead + 1;
      if (dayNum < 1 || dayNum > daysInMonth) {
        cells.push({ day: '', isToday: false, hasEv: false, evLabel: '' });
      } else {
        const evs = eventsByDay.get(dayNum) || [];
        cells.push({
          day: '' + dayNum,
          isToday: isCurrentMonth && today.getDate() === dayNum,
          hasEv: evs.length > 0,
          evLabel: evs.length ? (evs.length > 1 ? `${evs[0].titre} +${evs.length - 1}` : evs[0].titre) : ''
        });
      }
    }
    return cells;
  }

  get upcoming(): UpcomingEvent[] {
    return [...this.evenements]
      .sort((a, b) => new Date(a.dateDebut).getTime() - new Date(b.dateDebut).getTime())
      .map(e => {
        const d = new Date(e.dateDebut);
        return {
          day: d.getDate().toString().padStart(2, '0'),
          mon: d.toLocaleDateString('fr-FR', { month: 'short' }).replace('.', ''),
          title: e.titre,
          sub: `${e.clientNom ? e.clientNom + ' · ' : ''}${e.nombreParticipants} pers.`,
          time: `${d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })} · ${e.lieu}`
        };
      });
  }

  eventTone(i: number): string {
    return EVENT_TONES[i % EVENT_TONES.length];
  }

  openNew(): void {
    this.editingId = null;
    this.form.reset({ nombreParticipants: 0, montant: 0 });
    this.showDialog = true;
  }

  openEdit(ev: EvenementResponse): void {
    this.editingId = ev.id;
    this.form.patchValue({
      ...ev,
      dateDebut: new Date(ev.dateDebut),
      dateFin: new Date(ev.dateFin)
    });
    this.showDialog = true;
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const v = this.form.value;
    const req = {
      ...v,
      dateDebut: v.dateDebut instanceof Date ? v.dateDebut.toISOString() : v.dateDebut,
      dateFin: v.dateFin instanceof Date ? v.dateFin.toISOString() : v.dateFin
    };
    const obs = this.editingId
      ? this.evenementService.update(this.editingId, req)
      : this.evenementService.create(req);
    obs.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: this.editingId ? 'Événement modifié' : 'Événement créé' });
        this.showDialog = false; this.saving = false; this.load();
      },
      error: () => {
        this.saving = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Opération échouée' });
      }
    });
  }

  delete(ev: EvenementResponse): void {
    this.confirmationService.confirm({
      message: `Supprimer l'événement "${ev.titre}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-trash',
      accept: () => {
        this.evenementService.delete(ev.id).subscribe({
          next: () => { this.messageService.add({ severity: 'warn', summary: 'Supprimé', detail: 'Événement supprimé' }); this.load(); },
          error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Suppression impossible' })
        });
      }
    });
  }

  isInvalid(f: string): boolean {
    const c = this.form.get(f); return !!(c && c.invalid && c.touched);
  }

  formatMontant(v: number): string {
    return new Intl.NumberFormat('fr-CI', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(v || 0);
  }
}
