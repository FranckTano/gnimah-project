package com.gnimah.backend.dto.reservation;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AcompteRequest {
    @NotNull(message = "Le montant de l acompte est requis")
    private BigDecimal montant;
    @NotNull(message = "Le mode de reglement est requis")
    private String mode;
    private String referenceTransaction;
    private Long caisseId;
    private String notes;
}
