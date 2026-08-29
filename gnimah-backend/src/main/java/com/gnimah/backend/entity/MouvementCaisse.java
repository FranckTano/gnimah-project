package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Journal unique de tous les flux d'argent de la résidence. Toute recette et
 * toute dépense y laissent une ligne : c'est la source de vérité que la
 * comptabilité et les versements exploitent.
 *
 * Le fait générateur est référencé par {@code origineType} / {@code origineId}
 * (paiement, dépense, versement, commande…) plutôt que par une clé étrangère
 * par type : cela éviterait une demi-douzaine de colonnes toujours nulles.
 */
@Entity
@Table(name = "mouvements_caisse")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MouvementCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionCaisse session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SensMouvement sens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeMouvementCaisse type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModePaiement mode = ModePaiement.ESPECES;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(length = 100)
    private String reference;

    @Column(name = "origine_type", length = 40)
    private String origineType;

    @Column(name = "origine_id")
    private Long origineId;

    /** Solde de la caisse juste après ce mouvement — fige l'historique. */
    @Column(name = "solde_apres", precision = 14, scale = 2)
    private BigDecimal soldeApres;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Builder.Default
    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;
}
