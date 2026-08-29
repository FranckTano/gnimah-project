package com.gnimah.backend.dto.sejour;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SejourRequest {
    @NotNull(message = "Le client est requis")
    private Long clientId;
    @NotNull(message = "La chambre est requise")
    private Long chambreId;
    @NotNull(message = "Le type de location est requis")
    private String typeLocation;
    @NotNull(message = "La date d'entrée est requise")
    private LocalDate dateEntree;
    private LocalDate dateSortie;
    private LocalTime heureEntree;
    private LocalTime heureSortie;

    /**
     * Tarif unitaire négocié pour ce séjour. Laissé vide, le tarif de la
     * chambre s'applique. Renseigné, il se substitue au tarif catalogue —
     * c'est la façon la plus directe d'accorder un prix particulier.
     */
    private BigDecimal tarifPersonnalise;

    /** POURCENTAGE ou MONTANT. */
    private String remiseType;
    /** 15 pour « 15 % », ou 5000 pour « 5 000 F de remise ». */
    private BigDecimal remiseValeur;
    private String remiseMotif;

    private BigDecimal montantPaye;
    private String modePaiement;
    private String notes;
    private Long reservationId;
    /** Caisse qui encaisse l'acompte d'arrivée. */
    private Long caisseId;
}
