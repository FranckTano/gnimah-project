package com.gnimah.backend.dto.reservation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponse {
    private Long id;
    private String numeroReservation;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long chambreId;
    private String chambreNumero;
    private String typeChambre;
    private LocalDateTime dateArrivee;
    private LocalDateTime dateDepart;
    private Integer nbNuits;

    private BigDecimal montantBrut;
    private String remiseType;
    private BigDecimal remiseValeur;
    private BigDecimal remiseMontant;
    private String remiseMotif;
    private BigDecimal montantPrevu;

    private BigDecimal acompte;
    private BigDecimal acompteRequis;
    private BigDecimal resteAcompte;
    private LocalDateTime dateAcompte;
    private String modeAcompte;
    private String referenceAcompte;
    /** Vrai quand l'acompte encaissé atteint le minimum exigé. */
    private boolean acompteSuffisant;

    private LocalDateTime dateLimiteAnnulation;
    /** Vrai tant que la limite des 48 h n'est pas franchie. */
    private boolean annulableSansFrais;
    private LocalDateTime dateAnnulation;
    private String motifAnnulation;
    private BigDecimal penaliteAnnulation;
    private BigDecimal montantRembourse;
    private String annuleParNom;

    private String statut;
    private String notes;
    private LocalDateTime createdAt;
}
