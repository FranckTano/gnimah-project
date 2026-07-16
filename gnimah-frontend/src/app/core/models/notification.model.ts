export interface NotificationResponse {
  id: number;
  type: string;
  titre: string;
  message: string;
  lien: string | null;
  lu: boolean;
  createdAt: string;
}

export const NOTIFICATION_ICONS: Record<string, string> = {
  NOUVELLE_RESERVATION: 'ti-calendar-plus',
  ARRIVEE_PREVUE: 'ti-door-enter',
  FIN_SEJOUR: 'ti-door-exit',
  MAINTENANCE: 'ti-tool',
  NOUVEL_EVENEMENT: 'ti-calendar-event'
};
