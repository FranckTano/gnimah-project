package com.gnimah.backend.dto.caisse;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MouvementCaisseRequest {
    private Long caisseId;
    @NotNull(message = "Le sens du mouvement est requis")
    private String sens;
    @NotNull(message = "Le type de mouvement est requis")
    private String type;
    @NotNull(message = "Le montant est requis")
    private BigDecimal montant;
    private String mode;
    private String libelle;
    private String reference;
    private String notes;
}
