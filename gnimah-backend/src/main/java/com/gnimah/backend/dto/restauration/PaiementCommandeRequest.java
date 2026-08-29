package com.gnimah.backend.dto.restauration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaiementCommandeRequest {
    @NotNull(message = "Le mode de paiement est requis")
    private String mode;
    private String referenceTransaction;
    /** Si vrai, l addition est portee au compte du sejour au lieu d etre encaissee. */
    private Boolean imputeSejour;
    private String notes;
}
