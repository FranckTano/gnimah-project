package com.gnimah.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PasswordResetRequestResponse {
    private Long id;
    private Long utilisateurId;
    private String utilisateurNomComplet;
    private String username;
    private String statut;
    private LocalDateTime createdAt;
}
