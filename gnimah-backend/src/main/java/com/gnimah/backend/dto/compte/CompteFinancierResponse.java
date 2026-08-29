package com.gnimah.backend.dto.compte;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompteFinancierResponse {
    private Long id;
    private String libelle;
    private String type;
    private String etablissement;
    private String numeroCompte;
    private String numeroTelephone;
    private String identifiant;
    private String titulaire;
    private BigDecimal solde;
    private boolean actif;
    private String notes;
}
