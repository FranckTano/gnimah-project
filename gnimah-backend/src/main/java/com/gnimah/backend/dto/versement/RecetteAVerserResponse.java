package com.gnimah.backend.dto.versement;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecetteAVerserResponse {
    /** Assiette proposee pour un versement : recette encaissee non encore reversee. */
    private Long caisseId;
    private String caisseNom;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    private BigDecimal recetteEncaissee;
    private BigDecimal dejaVerse;
    private BigDecimal resteAVerser;
    private BigDecimal soldeCaisse;
}
