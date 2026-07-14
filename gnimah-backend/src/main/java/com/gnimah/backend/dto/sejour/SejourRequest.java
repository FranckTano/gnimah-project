package com.gnimah.backend.dto.sejour;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class SejourRequest {
    @NotNull(message = "Le client est requis")
    private Long clientId;
    @NotNull(message = "La chambre est requise")
    private Long chambreId;
    @NotNull(message = "Le type de location est requis")
    private String typeLocation;
    @NotNull(message = "La date d'entrée est requise")
    private LocalDateTime dateEntree;
    private LocalDateTime dateSortie;
    private LocalTime heureEntree;
    private LocalTime heureSortie;
    private BigDecimal montantPaye;
    private String notes;
    private Long reservationId;
}
