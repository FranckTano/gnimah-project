package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.ModePaiement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sejour_id", nullable = false)
    private Sejour sejour;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModePaiement mode = ModePaiement.ESPECES;

    @Column(name = "reference_transaction", length = 100)
    private String referenceTransaction;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
