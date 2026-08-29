package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.StatutSessionCaisse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une vacation de caisse : de l'ouverture par la caissière (avec son fond de
 * caisse) jusqu'au comptage de fermeture. L'écart entre le montant théorique
 * et le montant physiquement compté est conservé — c'est la pièce que la
 * comptabilité examine en cas de manquant.
 */
@Entity
@Table(name = "sessions_caisse")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caissier_id")
    private Utilisateur caissier;

    @Builder.Default
    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture = LocalDateTime.now();

    @Builder.Default
    @Column(name = "fond_caisse", nullable = false, precision = 14, scale = 2)
    private BigDecimal fondCaisse = BigDecimal.ZERO;

    @Column(name = "date_fermeture")
    private LocalDateTime dateFermeture;

    @Builder.Default
    @Column(name = "total_entrees", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEntrees = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_sorties", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSorties = BigDecimal.ZERO;

    /** Fond + entrées − sorties : ce que la caisse devrait contenir. */
    @Builder.Default
    @Column(name = "montant_theorique", nullable = false, precision = 14, scale = 2)
    private BigDecimal montantTheorique = BigDecimal.ZERO;

    /** Ce que la caissière compte réellement à la fermeture. */
    @Column(name = "montant_compte", precision = 14, scale = 2)
    private BigDecimal montantCompte;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ecart = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSessionCaisse statut = StatutSessionCaisse.OUVERTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ferme_par_id")
    private Utilisateur fermePar;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
