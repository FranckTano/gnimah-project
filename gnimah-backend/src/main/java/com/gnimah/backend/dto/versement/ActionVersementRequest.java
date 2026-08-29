package com.gnimah.backend.dto.versement;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ActionVersementRequest {
    private BigDecimal montant;
    private String referenceTransaction;
    private String commentaire;
}
