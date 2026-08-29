package com.gnimah.backend.dto.stock;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MouvementStockRequest {
    @NotNull(message = "L article est requis")
    private Long articleId;
    @NotNull(message = "Le type de mouvement est requis")
    private String type;
    @NotNull(message = "La quantite est requise")
    private BigDecimal quantite;
    private BigDecimal prixUnitaire;
    private Long fournisseurId;
    private Long chambreId;
    /** Pour un achat : genere aussi la depense correspondante et le decaissement. */
    private Boolean genererDepense;
    private Long caisseId;
    private String modeReglement;
    private String motif;
}
