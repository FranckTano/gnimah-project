package com.gnimah.backend.dto.restauration;

import lombok.Data;

@Data
public class PaiementCommandeRequest {
    /** Requis pour un encaissement ; sans objet quand l'addition est portée à la chambre. */
    private String mode;
    private String referenceTransaction;
    /** Si vrai, l addition est portee au compte du sejour au lieu d etre encaissee. */
    private Boolean imputeSejour;
    private String notes;
}
