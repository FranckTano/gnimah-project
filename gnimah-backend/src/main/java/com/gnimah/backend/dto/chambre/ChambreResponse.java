package com.gnimah.backend.dto.chambre;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChambreResponse {
    private Long id;
    private String numero;
    private String type;
    private int capacite;
    private BigDecimal tarifPassage;
    private BigDecimal tarifNuitee;
    private String etat;
    private int etage;
    private String description;
    private String equipements;
    private String vue;
    private String observations;
    private String photos;
    private boolean actif;
    private boolean disponible;
}
