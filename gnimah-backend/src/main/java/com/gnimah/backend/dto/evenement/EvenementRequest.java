package com.gnimah.backend.dto.evenement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EvenementRequest {
    @NotBlank(message = "Le titre est requis")
    private String titre;
    private String description;
    @NotNull(message = "La date de début est requise")
    private LocalDateTime dateDebut;
    @NotNull(message = "La date de fin est requise")
    private LocalDateTime dateFin;
    @NotBlank(message = "Le lieu est requis")
    private String lieu;
    private String typeEvenement;
    private Integer nombreParticipants;
    private BigDecimal montant;
    private Long clientId;
}
