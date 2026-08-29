package com.gnimah.backend.dto.fournisseur;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FournisseurResponse {
    private Long id;
    private String nom;
    private String type;
    private String contact;
    private String telephone;
    private String email;
    private String adresse;
    private String notes;
    private boolean actif;
}
