package com.gnimah.backend.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReservationRequest {
    @NotNull
    private Long clientId;
    private Long chambreId;
    private String typeChambre;
    @NotNull
    private LocalDate dateArrivee;
    @NotNull
    private LocalDate dateDepart;

    /** Tarif de nuitée négocié ; à défaut, le tarif de la chambre s'applique. */
    private BigDecimal tarifPersonnalise;
    /** POURCENTAGE ou MONTANT. */
    private String remiseType;
    private BigDecimal remiseValeur;
    private String remiseMotif;

    /**
     * Acompte attendu pour confirmer. Laissé vide, la résidence applique son
     * taux par défaut (30 % du montant prévu).
     */
    private BigDecimal acompteRequis;

    /**
     * Acompte encaissé dès la création. Suffisant, il confirme immédiatement la
     * réservation ; sinon elle reste en attente.
     */
    private BigDecimal acompte;
    private String modeAcompte;
    private String referenceAcompte;
    private Long caisseId;

    private String notes;
}
