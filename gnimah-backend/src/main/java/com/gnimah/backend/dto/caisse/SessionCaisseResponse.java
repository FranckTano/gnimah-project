package com.gnimah.backend.dto.caisse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionCaisseResponse {
    private Long id;
    private Long caisseId;
    private String caisseNom;
    private Long caissierId;
    private String caissierNom;
    private LocalDateTime dateOuverture;
    private BigDecimal fondCaisse;
    private LocalDateTime dateFermeture;
    /** Activité de la vacation, tous modes de règlement (hors régularisations du fond). */
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    /** Part en espèces de cette activité : c'est elle qui fait varier le tiroir. */
    private BigDecimal entreesEspeces;
    private BigDecimal sortiesEspeces;
    /** Fond de caisse + entrées en espèces − sorties en espèces. */
    private BigDecimal montantTheorique;
    private BigDecimal montantCompte;
    private BigDecimal ecart;
    private String statut;
    private String fermeParNom;
    private String notes;
    private long nbMouvements;
}
