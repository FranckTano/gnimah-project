package com.gnimah.backend.dto.restauration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommandeResponse {
    private Long id;
    private String numero;
    private String type;
    private String tableNumero;
    private Long sejourId;
    private String sejourChambre;
    private Long clientId;
    private String clientNom;
    private Long evenementId;
    private String evenementTitre;
    private BigDecimal montantBrut;
    private String remiseType;
    private BigDecimal remiseValeur;
    private BigDecimal remiseMontant;
    private String remiseMotif;
    private BigDecimal montantNet;
    private BigDecimal montantPaye;
    private String statut;
    private String modePaiement;
    private boolean imputeSejour;
    private String agentNom;
    private LocalDateTime dateCommande;
    private LocalDateTime datePaiement;
    private String notes;
    private List<LigneCommandeResponse> lignes;
}
