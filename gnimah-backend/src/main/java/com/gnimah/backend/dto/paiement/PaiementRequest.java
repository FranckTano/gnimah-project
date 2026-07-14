package com.gnimah.backend.dto.paiement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaiementRequest {
    @NotNull
    private Long sejourId;
    @NotNull @Positive
    private BigDecimal montant;
    @NotNull
    private String mode;
    private String referenceTransaction;
    private String notes;
}
