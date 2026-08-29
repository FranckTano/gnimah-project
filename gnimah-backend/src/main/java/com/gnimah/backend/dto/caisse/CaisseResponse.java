package com.gnimah.backend.dto.caisse;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaisseResponse {
    private Long id;
    private String nom;
    private String type;
    private BigDecimal solde;
    private Long responsableId;
    private String responsableNom;
    private String description;
    private boolean actif;
    /** Session ouverte en cours, si la caisse est en service. */
    private Long sessionOuverteId;
    private String sessionCaissierNom;
}
