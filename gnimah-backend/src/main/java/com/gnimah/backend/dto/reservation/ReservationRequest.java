package com.gnimah.backend.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReservationRequest {
    @NotNull
    private Long clientId;
    private Long chambreId;
    private String typeChambre;
    @NotNull
    private LocalDateTime dateArrivee;
    @NotNull
    private LocalDateTime dateDepart;
    private BigDecimal acompte;
    private String notes;
}
