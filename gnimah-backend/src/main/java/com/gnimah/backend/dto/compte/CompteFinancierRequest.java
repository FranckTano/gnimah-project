package com.gnimah.backend.dto.compte;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompteFinancierRequest {
    @NotBlank(message = "Le libelle du compte est requis")
    private String libelle;
    private String type;
    private String etablissement;
    private String numeroCompte;
    private String numeroTelephone;
    private String titulaire;
    private String notes;
    private Boolean actif;
}
