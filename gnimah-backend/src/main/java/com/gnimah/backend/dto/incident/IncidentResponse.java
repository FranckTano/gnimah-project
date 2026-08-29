package com.gnimah.backend.dto.incident;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IncidentResponse {
    private Long id;
    private String numero;
    private Long chambreId;
    private String chambreNumero;
    private Long sejourId;
    private String sejourNumeroRecu;
    private Long clientId;
    private String clientNom;
    private String type;
    private String designation;
    private Long articleStockId;
    private String articleDesignation;
    private Integer quantite;
    private String gravite;
    private String description;
    private String photo;
    private String statut;
    private BigDecimal coutEstime;
    private BigDecimal coutReel;
    private boolean imputeClient;
    private String declareParNom;
    private String resoluParNom;
    private Long tacheId;
    private Long depenseId;
    private LocalDateTime dateIncident;
    private LocalDateTime dateResolution;
    private String notes;
}
