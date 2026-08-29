package com.gnimah.backend.dto.incident;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ResolutionIncidentRequest {
    @NotNull(message = "Le statut de resolution est requis")
    private String statut;
    private BigDecimal coutReel;
    /** Vrai : le cout est ajoute au sejour du client. Faux : il devient une charge. */
    private Boolean imputeClient;
    /** Famille de charge a utiliser si le cout reste a la charge de la residence. */
    private String familleDepense;
    private Long caisseId;
    private String notes;
    /** Remet la chambre en etat LIBRE une fois la reparation faite. */
    private Boolean libererChambre;
}
