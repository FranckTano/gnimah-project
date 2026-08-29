package com.gnimah.backend.dto.caisse;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class OuvertureCaisseRequest {
    @NotNull(message = "La caisse est requise")
    private Long caisseId;
    /** Especes physiquement presentes a l ouverture de la vacation. */
    private BigDecimal fondCaisse;
    private String notes;
}
