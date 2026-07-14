package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.TypeLocation;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sejours")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sejour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_recu", nullable = false, unique = true, length = 50)
    private String numeroRecu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    private Chambre chambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_location", nullable = false, length = 20)
    private TypeLocation typeLocation = TypeLocation.SEJOUR;

    @Column(name = "date_entree", nullable = false)
    private LocalDateTime dateEntree;

    @Column(name = "date_sortie")
    private LocalDateTime dateSortie;

    @Column(name = "heure_entree")
    private LocalTime heureEntree;

    @Column(name = "heure_sortie")
    private LocalTime heureSortie;

    @Column(name = "nb_jours")
    private Integer nbJours;

    @Column(name = "nb_heures")
    private Integer nbHeures;

    @Column(name = "montant_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "montant_paye", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "reste_a_payer", nullable = false, precision = 10, scale = 2)
    private BigDecimal resteAPayer = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String statut = "EN_COURS";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reservation_id")
    private Long reservationId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
