package com.gnimah.backend.dto.restauration;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LigneCommandeResponse {
    private Long id;
    private Long articleId;
    private String designation;
    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montant;
}
