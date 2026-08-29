package com.gnimah.backend.dto.restauration;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class LigneCommandeRequest {
    private Long articleId;
    /** Libelle libre, pour un article hors carte. */
    private String designation;
    @NotNull(message = "La quantite est requise")
    private Integer quantite;
    private BigDecimal prixUnitaire;
}
