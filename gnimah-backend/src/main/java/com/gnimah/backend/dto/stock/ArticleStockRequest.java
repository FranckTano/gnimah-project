package com.gnimah.backend.dto.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ArticleStockRequest {
    private String code;
    @NotBlank(message = "La designation est requise")
    private String designation;
    @NotNull(message = "La categorie est requise")
    private String categorie;
    private String unite;
    /** Quantite initiale ; ignoree lors d une modification. */
    private BigDecimal quantite;
    private BigDecimal seuilAlerte;
    private BigDecimal prixUnitaire;
    private Long fournisseurId;
    private String emplacement;
    private Boolean actif;
}
