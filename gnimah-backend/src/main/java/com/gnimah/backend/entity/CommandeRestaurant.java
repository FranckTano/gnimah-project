package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.StatutCommande;
import com.gnimah.backend.entity.enums.TypeCommande;
import com.gnimah.backend.entity.enums.TypeRemise;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Une addition du restaurant. Elle peut être réglée immédiatement en caisse ou
 * imputée à un séjour en cours (room service), auquel cas son montant vient
 * s'ajouter au reste à payer de la chambre et sera encaissé au départ.
 */
@Entity
@Table(name = "commandes_restaurant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandeRestaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeCommande type = TypeCommande.SUR_PLACE;

    @Column(name = "table_numero", length = 20)
    private String tableNumero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sejour_id")
    private Sejour sejour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    @Builder.Default
    @Column(name = "montant_brut", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantBrut = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "remise_type", length = 20)
    private TypeRemise remiseType;

    @Builder.Default
    @Column(name = "remise_valeur", nullable = false, precision = 12, scale = 2)
    private BigDecimal remiseValeur = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "remise_montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal remiseMontant = BigDecimal.ZERO;

    @Column(name = "remise_motif", length = 255)
    private String remiseMotif;

    @Builder.Default
    @Column(name = "montant_net", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantNet = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "montant_paye", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCommande statut = StatutCommande.OUVERTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 30)
    private ModePaiement modePaiement;

    @Builder.Default
    @Column(name = "impute_sejour", nullable = false)
    private boolean imputeSejour = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;

    @Builder.Default
    @Column(name = "date_commande", nullable = false)
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes = new ArrayList<>();
}
