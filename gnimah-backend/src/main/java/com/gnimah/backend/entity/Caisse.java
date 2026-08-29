package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.TypeCaisse;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caisses")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Caisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeCaisse type = TypeCaisse.RECEPTION;

    /**
     * Espèces détenues, recalculées à chaque mouvement — jamais saisies à la main.
     *
     * Seul le numéraire est compté ici : un encaissement Wave ou par carte est
     * une recette de la résidence et figure au journal, mais il n'arrive pas dans
     * le tiroir et ne peut donc pas être versé en banque par la caissière.
     */
    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Utilisateur responsable;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
