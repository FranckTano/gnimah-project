package com.gnimah.backend.dto.versement;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

@Data
public class VersementRequest {
    @NotNull(message = "La caisse est requise")
    private Long caisseId;
    @NotNull(message = "Le compte de destination est requis")
    private Long compteId;
    private String periodicite;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    @NotNull(message = "Le montant du versement est requis")
    private BigDecimal montant;
    @NotNull(message = "La date de versement est requise")
    private LocalDate dateVersement;
    private LocalTime heureVersement;
    private String mode;
    private String referenceTransaction;
    private String justificatifUrl;
    private String notes;
}
