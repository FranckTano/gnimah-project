package com.gnimah.backend.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ClientRequest {
    @NotBlank(message = "La civilité est requise")
    private String civilite;
    @NotBlank(message = "Le nom est requis")
    private String nom;
    private String prenom;
    @NotBlank(message = "Le téléphone est requis")
    private String telephone;
    private String email;
    @NotBlank(message = "Le type de pièce est requis")
    private String typePiece;
    @NotBlank(message = "Le numéro de pièce est requis")
    private String numeroPiece;
    private String nationalite;
    private String adresse;
}
