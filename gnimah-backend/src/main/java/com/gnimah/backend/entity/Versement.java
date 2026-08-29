package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.PeriodiciteVersement;
import com.gnimah.backend.entity.enums.StatutVersement;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dépôt de la recette d'une caisse vers un compte bancaire ou un numéro mobile
 * money. La caissière prépare le versement pour une période (journée, semaine
 * ou mois), enregistre la date de dépôt effective, puis la direction confirme.
 *
 * Le montant est figé à la préparation : si des encaissements arrivent après,
 * ils appartiennent au versement suivant, sinon le rapprochement avec le reçu
 * de la banque deviendrait impossible.
 */
@Entity
@Table(name = "versements")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Versement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private CompteFinancier compte;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodiciteVersement periodicite = PeriodiciteVersement.JOURNALIER;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_versement", nullable = false)
    private LocalDate dateVersement;

    @Column(name = "heure_versement")
    private LocalTime heureVersement;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModePaiement mode = ModePaiement.ESPECES;

    @Column(name = "reference_transaction", length = 100)
    private String referenceTransaction;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutVersement statut = StatutVersement.PREPARE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caissier_id")
    private Utilisateur caissier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "justificatif_url", columnDefinition = "TEXT")
    private String justificatifUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "versement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateAction ASC")
    private List<ActionVersement> actions = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
