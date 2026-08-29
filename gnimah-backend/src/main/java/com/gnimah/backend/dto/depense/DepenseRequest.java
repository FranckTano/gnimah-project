package com.gnimah.backend.dto.depense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DepenseRequest {
    @NotBlank(message = "Le libelle est requis")
    private String libelle;
    @NotNull(message = "Le montant est requis")
    private BigDecimal montant;
    @NotNull(message = "La famille de charge est requise")
    private String famille;
    /** Sous-poste libre a l interieur de la famille (ex : "Riz", "Ampoules"). */
    private String categorie;
    private LocalDate dateDepense;
    private Long fournisseurId;
    private String modeReglement;
    private String referencePiece;
    /** Laisse vide, la depense est enregistree en attente de validation. */
    private String statut;
    private Long caisseId;
    private Long chambreId;
    private Long sejourId;
    private String notes;
}
