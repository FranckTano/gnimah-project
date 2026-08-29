package com.gnimah.backend.dto.restauration;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CommandeRequest {
    private String type;
    private String tableNumero;
    /** Sejour a debiter pour un room service ; le montant rejoint le reste a payer. */
    private Long sejourId;
    private Long clientId;
    private Long evenementId;
    private Boolean imputeSejour;
    private String remiseType;
    private BigDecimal remiseValeur;
    private String remiseMotif;
    private String notes;
    @NotNull(message = "La commande doit comporter au moins une ligne")
    private List<LigneCommandeRequest> lignes;
}
