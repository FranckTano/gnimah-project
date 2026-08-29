package com.gnimah.backend.dto.restauration;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleRestaurationResponse {
    private Long id;
    private String code;
    private String nom;
    private String categorie;
    private String description;
    private BigDecimal prixVente;
    private BigDecimal coutRevient;
    private BigDecimal marge;
    private boolean disponible;
    private String photo;
}
