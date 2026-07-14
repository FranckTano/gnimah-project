package com.gnimah.backend.dto.paiement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaiementResponse {
    private Long id;
    private Long sejourId;
    private String clientNom;
    private String numeroChambre;
    private String numeroRecu;
    private BigDecimal montant;
    private String modePaiement;
    private String referenceTransaction;
    private LocalDateTime dateHeure;
    private String agentNom;
    private String notes;
}
