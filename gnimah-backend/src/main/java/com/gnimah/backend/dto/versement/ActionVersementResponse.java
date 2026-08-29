package com.gnimah.backend.dto.versement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionVersementResponse {
    private Long id;
    private String action;
    private LocalDateTime dateAction;
    private BigDecimal montant;
    private String agentNom;
    private String commentaire;
}
