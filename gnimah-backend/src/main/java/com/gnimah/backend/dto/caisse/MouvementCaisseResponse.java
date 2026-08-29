package com.gnimah.backend.dto.caisse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MouvementCaisseResponse {
    private Long id;
    private Long caisseId;
    private String caisseNom;
    private Long sessionId;
    private String sens;
    private String type;
    private BigDecimal montant;
    private String mode;
    private String libelle;
    private String reference;
    private String origineType;
    private Long origineId;
    private BigDecimal soldeApres;
    private String agentNom;
    private LocalDateTime dateMouvement;
    private String notes;
}
