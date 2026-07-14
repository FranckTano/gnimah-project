package com.gnimah.backend.dto.reservation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponse {
    private Long id;
    private String numeroReservation;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long chambreId;
    private String chambreNumero;
    private String typeChambre;
    private LocalDateTime dateArrivee;
    private LocalDateTime dateDepart;
    private Integer nbNuits;
    private BigDecimal montantPrevu;
    private BigDecimal acompte;
    private String statut;
    private String notes;
    private LocalDateTime createdAt;
}
