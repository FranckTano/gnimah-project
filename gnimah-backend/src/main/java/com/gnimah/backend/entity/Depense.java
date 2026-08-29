package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.FamilleDepense;
import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.StatutDepense;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une charge de la résidence : denrées du restaurant, provisions, matériel de
 * maintenance des chambres, salaires, énergie…
 *
 * Une dépense passe par un circuit de validation : saisie (EN_ATTENTE),
 * approuvée par la direction (VALIDEE), puis réglée (PAYEE). Seul le passage à
 * PAYEE sur une caisse génère une sortie d'espèces — ce qui évite qu'une simple
 * saisie ne fasse bouger la trésorerie.
 */
@Entity
@Table(name = "depenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Depense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    /**
     * Libellé libre historique (avant les familles normalisées). Conservé pour
     * ne pas perdre les saisies antérieures et pour permettre un sous-poste
     * plus fin que la famille.
     */
    @Builder.Default
    @Column(nullable = false, length = 100)
    private String categorie = "DIVERS";

    /** Poste comptable normalisé, base du compte de résultat. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FamilleDepense famille = FamilleDepense.DIVERS;

    @Builder.Default
    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_reglement", nullable = false, length = 30)
    private ModePaiement modeReglement = ModePaiement.ESPECES;

    /** N° de facture, de reçu ou de bon de commande du fournisseur. */
    @Column(name = "reference_piece", length = 100)
    private String referencePiece;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDepense statut = StatutDepense.EN_ATTENTE;

    /** Caisse ayant supporté le décaissement, quand le règlement est en espèces. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    /** Chambre concernée pour une dépense de maintenance ciblée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id")
    private Chambre chambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sejour_id")
    private Sejour sejour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
