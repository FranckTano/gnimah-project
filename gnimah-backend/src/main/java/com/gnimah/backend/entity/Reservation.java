package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeChambre;
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

    @Column(name = "montant_prevu", precision = 10, scale = 2)
    private BigDecimal montantPrevu;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal acompte = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
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
