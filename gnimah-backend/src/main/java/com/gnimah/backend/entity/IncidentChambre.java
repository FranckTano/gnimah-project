package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.GraviteIncident;
import com.gnimah.backend.entity.enums.StatutIncident;
import com.gnimah.backend.entity.enums.TypeIncident;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Déclaration d'un objet ou matériel défectueux, manquant ou détérioré, relevée
 * par le service d'entretien après le passage d'un client.
 *
 * L'incident conserve le lien vers le séjour concerné : c'est ce qui permet
 * d'imputer la réparation au client fautif plutôt qu'aux charges de la
 * résidence. À la résolution, l'incident produit soit une dépense de
 * maintenance, soit un supplément sur le séjour — jamais les deux.
 */
@Entity
@Table(name = "incidents_chambre")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentChambre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chambre_id", nullable = false)
    private Chambre chambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sejour_id")
    private Sejour sejour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeIncident type = TypeIncident.AUTRE;

    @Column(nullable = false, length = 200)
    private String designation;

    /** Article de stock concerné, quand le matériel est suivi en inventaire. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_stock_id")
    private ArticleStock articleStock;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantite = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GraviteIncident gravite = GraviteIncident.MINEUR;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String photo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutIncident statut = StatutIncident.DECLARE;

    @Builder.Default
    @Column(name = "cout_estime", nullable = false, precision = 12, scale = 2)
    private BigDecimal coutEstime = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cout_reel", nullable = false, precision = 12, scale = 2)
    private BigDecimal coutReel = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impute_client", nullable = false)
    private boolean imputeClient = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declare_par_id")
    private Utilisateur declarePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolu_par_id")
    private Utilisateur resoluPar;

    /** Tâche de maintenance ouverte automatiquement pour les incidents sérieux. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tache_id")
    private TacheEntretien tache;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depense_id")
    private Depense depense;

    @Builder.Default
    @Column(name = "date_incident", nullable = false)
    private LocalDateTime dateIncident = LocalDateTime.now();

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
