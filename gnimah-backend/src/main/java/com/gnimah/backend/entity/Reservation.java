package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.entity.enums.TypeRemise;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_reservation", nullable = false, unique = true, length = 50)
    private String numeroReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id")
    private Chambre chambre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_chambre", length = 20)
    private TypeChambre typeChambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Column(name = "date_arrivee", nullable = false)
    private LocalDateTime dateArrivee;

    @Column(name = "date_depart", nullable = false)
    private LocalDateTime dateDepart;

    @Column(name = "nb_nuits")
    private Integer nbNuits;

    /** Montant de l'hébergement avant remise. */
    @Column(name = "montant_brut", precision = 12, scale = 2)
    private BigDecimal montantBrut;

    @Enumerated(EnumType.STRING)
    @Column(name = "remise_type", length = 20)
    private TypeRemise remiseType;

    @Builder.Default
    @Column(name = "remise_valeur", nullable = false, precision = 12, scale = 2)
    private BigDecimal remiseValeur = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "remise_montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal remiseMontant = BigDecimal.ZERO;

    @Column(name = "remise_motif", length = 255)
    private String remiseMotif;

    /** Montant prévisionnel du séjour, remise déduite. */
    @Column(name = "montant_prevu", precision = 10, scale = 2)
    private BigDecimal montantPrevu;

    /** Acompte effectivement encaissé. */
    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal acompte = BigDecimal.ZERO;

    /**
     * Acompte attendu pour que la réservation puisse être confirmée. Figé à la
     * création : si le taux de la maison change, les engagements déjà pris
     * restent jugés sur la règle qui s'appliquait ce jour-là.
     */
    @Builder.Default
    @Column(name = "acompte_requis", nullable = false, precision = 12, scale = 2)
    private BigDecimal acompteRequis = BigDecimal.ZERO;

    @Column(name = "date_acompte")
    private LocalDateTime dateAcompte;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_acompte", length = 30)
    private ModePaiement modeAcompte;

    @Column(name = "reference_acompte", length = 100)
    private String referenceAcompte;

    /**
     * Date au-delà de laquelle une annulation devient pénalisante — 48 h avant
     * l'arrivée. Stockée, pas recalculée : la règle en vigueur au moment de
     * l'engagement fait foi.
     */
    @Column(name = "date_limite_annulation")
    private LocalDateTime dateLimiteAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    /** Part de l'acompte conservée par la résidence en cas d'annulation tardive. */
    @Builder.Default
    @Column(name = "penalite_annulation", nullable = false, precision = 12, scale = 2)
    private BigDecimal penaliteAnnulation = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "montant_rembourse", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantRembourse = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_id")
    private Utilisateur annulePar;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
