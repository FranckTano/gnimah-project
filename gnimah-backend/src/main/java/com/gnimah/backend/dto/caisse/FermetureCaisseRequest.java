package com.gnimah.backend.dto.caisse;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FermetureCaisseRequest {
    @NotNull(message = "Le montant compte est requis")
    /** Especes reellement comptees par la caissiere a la fermeture. */
    private BigDecimal montantCompte;
    private String notes;
}
