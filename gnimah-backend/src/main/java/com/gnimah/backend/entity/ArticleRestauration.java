package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.CategorieArticleResto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Une entrée de la carte du restaurant. */
@Entity
@Table(name = "articles_restauration")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleRestauration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategorieArticleResto categorie = CategorieArticleResto.PLAT;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prix_vente", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixVente;

    /** Coût matière estimé — sert au calcul de la marge par article. */
    @Builder.Default
    @Column(name = "cout_revient", nullable = false, precision = 12, scale = 2)
    private BigDecimal coutRevient = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean disponible = true;

    @Column(columnDefinition = "TEXT")
    private String photo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
