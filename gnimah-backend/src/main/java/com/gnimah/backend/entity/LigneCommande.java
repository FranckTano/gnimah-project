package com.gnimah.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne d'une addition. La désignation et le prix unitaire sont recopiés depuis
 * la carte au moment de la commande : changer le prix d'un plat ne doit pas
 * réécrire les additions déjà émises.
 */
@Entity
@Table(name = "lignes_commande")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeRestaurant commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private ArticleRestauration article;

    @Column(nullable = false, length = 150)
    private String designation;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantite = 1;

    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;
}
