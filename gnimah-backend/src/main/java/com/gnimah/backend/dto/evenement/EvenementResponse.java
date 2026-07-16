package com.gnimah.backend.dto.evenement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EvenementResponse {
    private Long id;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private String typeEvenement;
    private String statut;
    private int nombreParticipants;
    private BigDecimal montant;
    private Long clientId;
    private String clientNom;
}
