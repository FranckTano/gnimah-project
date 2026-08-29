package com.gnimah.backend.dto.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PolitiqueAnnulationResponse {
    /** Simulation de l annulation a l instant present, avant decision de l agent. */
    private Long reservationId;
    private LocalDateTime dateArrivee;
    private LocalDateTime dateLimiteAnnulation;
    private long heuresAvantArrivee;
    private boolean dansLesDelais;
    private BigDecimal acompteVerse;
    private BigDecimal penalite;
    private BigDecimal remboursement;
    private String message;
}
