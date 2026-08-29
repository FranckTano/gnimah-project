package com.gnimah.backend.dto.depense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepenseResponse {
    private Long id;
    private String libelle;
    private BigDecimal montant;
    private String famille;
    private String categorie;
    private LocalDate dateDepense;
    private Long fournisseurId;
    private String fournisseurNom;
    private String modeReglement;
    private String referencePiece;
    private String statut;
    private Long caisseId;
    private String caisseNom;
    private Long chambreId;
    private String chambreNumero;
    private Long sejourId;
    private String agentNom;
    private String valideParNom;
    private LocalDateTime dateValidation;
    private String notes;
    private LocalDateTime createdAt;
}
