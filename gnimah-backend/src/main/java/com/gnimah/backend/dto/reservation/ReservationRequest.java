package com.gnimah.backend.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReservationRequest {
    @NotNull
    private Long clientId;
    private Long chambreId;
    private String typeChambre;
    @NotNull
    private LocalDate dateArrivee;
    @NotNull
    private LocalDate dateDepart;
    private BigDecimal acompte;
    private String notes;
}
