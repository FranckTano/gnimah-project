package com.gnimah.backend.dto.restauration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ArticleRestaurationRequest {
    private String code;
    @NotBlank(message = "Le nom de l article est requis")
    private String nom;
    @NotNull(message = "La categorie est requise")
    private String categorie;
    private String description;
    @NotNull(message = "Le prix de vente est requis")
    private BigDecimal prixVente;
    private BigDecimal coutRevient;
    private Boolean disponible;
    private String photo;
}
