package com.gnimah.backend.dto.sejour;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class SejourResponse {
    private Long id;
    private String numeroRecu;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long chambreId;
    private String chambreNumero;
    private String chambreType;
    private String agentNom;
    private String typeLocation;
    private LocalDateTime dateEntree;
    private LocalDateTime dateSortie;
    private LocalTime heureEntree;
    private LocalTime heureSortie;
    private Integer nbJours;
    private Integer nbHeures;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private String statut;
    private String notes;
    private LocalDateTime createdAt;
}
