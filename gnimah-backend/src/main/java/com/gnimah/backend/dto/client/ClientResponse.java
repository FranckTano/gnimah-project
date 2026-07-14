package com.gnimah.backend.dto.client;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClientResponse {
    private Long id;
    private String civilite;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String telephone;
    private String email;
    private String typePiece;
    private String numeroPiece;
    private String nationalite;
    private String adresse;
    private int nbSejours;
    private boolean clientFidele;
    private LocalDateTime createdAt;
}
