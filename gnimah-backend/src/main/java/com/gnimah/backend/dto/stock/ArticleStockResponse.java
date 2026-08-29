package com.gnimah.backend.dto.stock;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleStockResponse {
    private Long id;
    private String code;
    private String designation;
    private String categorie;
    private String unite;
    private BigDecimal quantite;
    private BigDecimal seuilAlerte;
    private BigDecimal prixUnitaire;
    private BigDecimal valeurStock;
    private Long fournisseurId;
    private String fournisseurNom;
    private String emplacement;
    private boolean actif;
    private boolean enAlerte;
}
