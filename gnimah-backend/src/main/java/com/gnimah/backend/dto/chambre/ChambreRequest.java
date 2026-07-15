package com.gnimah.backend.dto.chambre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChambreRequest {
    @NotBlank(message = "Le numéro de chambre est requis")
    private String numero;
    @NotBlank(message = "Le type est requis")
    private String type;
    @Positive
    private int capacite = 2;
    @NotNull @Positive
    private BigDecimal tarifPassage;
    @NotNull @Positive
    private BigDecimal tarifNuitee;
    private String etat;
    private int etage;
    private String description;
    private String equipements;
    private String vue;
    private String observations;
    private String photos;
}
