package com.gnimah.backend.dto.utilisateur;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UtilisateurResponse {
    private Long id;
    private String username;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role;
    private boolean actif;
    private LocalDateTime dateCreation;
}
