package com.gnimah.backend.dto.entretien;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TacheEntretienResponse {
    private Long id;
    private String titre;
    private Long chambreId;
    private String chambreNumero;
    private String salle;
    private Long evenementId;
    private String evenementTitre;
    private String type;
    private String description;
    private String statut;
    private String priorite;
    private Long agentId;
    private String assigneA;
    private Long responsableId;
    private String responsableNom;
    private LocalDateTime dateLimite;
    private LocalDateTime dateCreation;
    private LocalDateTime dateFin;
}
