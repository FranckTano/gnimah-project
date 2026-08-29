package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.TypeActionVersement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une étape du cycle de vie d'un versement, horodatée et signée.
 *
 * Le nom de l'agent est dupliqué en clair : la ligne doit rester lisible même
 * si le compte utilisateur est désactivé ou renommé plus tard — c'est une
 * pièce justificative, pas un simple affichage.
 */
@Entity
@Table(name = "actions_versement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActionVersement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versement_id", nullable = false)
    private Versement versement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeActionVersement action;

    @Builder.Default
    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    @Column(precision = 14, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Column(name = "agent_nom", length = 150)
    private String agentNom;

    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
