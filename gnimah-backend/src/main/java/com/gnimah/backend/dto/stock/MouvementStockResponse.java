package com.gnimah.backend.dto.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MouvementStockResponse {
    private Long id;
    private Long articleId;
    private String articleDesignation;
    private String unite;
    private String sens;
    private String type;
    private BigDecimal quantite;
    private BigDecimal quantiteApres;
    private BigDecimal prixUnitaire;
    private BigDecimal montant;
    private String fournisseurNom;
    private Long depenseId;
    private String chambreNumero;
    private String agentNom;
    private LocalDateTime dateMouvement;
    private String motif;
}
