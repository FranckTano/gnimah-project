export interface PublicHotelInfo {
  totalChambres: number;
  /** Version de l'application servie par le backend. */
  version?: string | null;
  /** Horodatage du build en service. */
  dateBuild?: string | null;
}
