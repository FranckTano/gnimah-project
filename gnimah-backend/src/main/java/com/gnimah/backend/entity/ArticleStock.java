package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.CategorieStock;
import com.gnimah.backend.entity.enums.UniteMesure;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Article détenu en stock : provisions de cuisine, boissons, produits
 * d'entretien, linge, petit matériel de chambre et de maintenance.
 */
@Entity
@Table(name = "articles_stock")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String designation;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategorieStock categorie = CategorieStock.AUTRE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UniteMesure unite = UniteMesure.PIECE;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite = BigDecimal.ZERO;

    /** En dessous de ce seuil, l'article remonte dans les alertes de réapprovisionnement. */
    @Builder.Default
    @Column(name = "seuil_alerte", nullable = false, precision = 12, scale = 2)
    private BigDecimal seuilAlerte = BigDecimal.ZERO;

    /** Dernier prix d'achat connu — valorise le stock et pré-remplit les sorties. */
    @Builder.Default
    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @Column(length = 100)
    private String emplacement;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isEnAlerte() {
        return quantite != null && seuilAlerte != null && quantite.compareTo(seuilAlerte) <= 0;
    }

    public BigDecimal getValeurStock() {
        if (quantite == null || prixUnitaire == null) return BigDecimal.ZERO;
        return quantite.multiply(prixUnitaire);
    }
}
