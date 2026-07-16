package com.gnimah.backend.dto.entretien;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TacheEntretienRequest {
    @NotBlank(message = "Le titre est requis")
    private String titre;
    @NotBlank(message = "Le type est requis")
    private String type;
    @NotBlank(message = "La description est requise")
    private String description;

    /** Cible optionnelle : au plus un des trois champs suivants est renseigné. */
    private Long chambreId;
    private String salle;
    private Long evenementId;

    @NotNull(message = "L'agent assigné est requis")
    private Long agentId;
    private String priorite;
    private LocalDateTime dateLimite;
}
