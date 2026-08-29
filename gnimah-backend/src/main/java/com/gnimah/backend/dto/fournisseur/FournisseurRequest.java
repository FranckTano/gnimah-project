package com.gnimah.backend.dto.fournisseur;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FournisseurRequest {
    @NotBlank(message = "Le nom du fournisseur est requis")
    private String nom;
    private String type;
    private String contact;
    private String telephone;
    private String email;
    private String adresse;
    private String notes;
    private Boolean actif;
}
