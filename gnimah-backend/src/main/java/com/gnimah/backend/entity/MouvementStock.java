package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.TypeMouvementStock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrée ou sortie de stock. Chaque ligne fige la quantité restante après le
 * mouvement : l'inventaire se relit alors sans rejouer tout l'historique.
 */
@Entity
@Table(name = "mouvements_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private ArticleStock article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SensMouvement sens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeMouvementStock type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite;

    @Column(name = "quantite_apres", precision = 12, scale = 2)
    private BigDecimal quantiteApres;

    @Builder.Default
    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    /** Dépense générée par un achat de provisions, le cas échéant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depense_id")
    private Depense depense;

    /** Chambre destinataire pour une sortie de matériel ou de linge. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id")
    private Chambre chambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Builder.Default
    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String motif;
}
