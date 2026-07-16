package com.gnimah.backend.dto.utilisateur;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UtilisateurRequest {
    @NotBlank(message = "Le nom d'utilisateur est requis")
    private String username;
    private String password;
    @NotBlank(message = "Le nom est requis")
    private String nom;
    @NotBlank(message = "Le prénom est requis")
    private String prenom;
    private String email;
    private String telephone;
    @NotBlank(message = "Le rôle est requis")
    private String role;
    private boolean actif = true;
}
