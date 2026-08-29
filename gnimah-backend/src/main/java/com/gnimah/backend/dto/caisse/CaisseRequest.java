package com.gnimah.backend.dto.caisse;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaisseRequest {
    @NotBlank(message = "Le nom de la caisse est requis")
    private String nom;
    private String type;
    private Long responsableId;
    private String description;
    private Boolean actif;
}
