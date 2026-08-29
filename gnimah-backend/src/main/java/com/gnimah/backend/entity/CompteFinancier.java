package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.TypeCompteFinancier;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Destination d'un versement de recette : compte bancaire ou numéro mobile
 * money. Les deux partagent la même table parce qu'ils jouent exactement le
 * même rôle vis-à-vis de la caisse — seul le champ d'identification change.
 */
@Entity
@Table(name = "comptes_financiers")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompteFinancier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeCompteFinancier type = TypeCompteFinancier.BANQUE;

    /** Nom de la banque, ou de l'opérateur mobile money (Wave, Orange Money…). */
    @Column(length = 150)
    private String etablissement;

    @Column(name = "numero_compte", length = 80)
    private String numeroCompte;

    @Column(name = "numero_telephone", length = 30)
    private String numeroTelephone;

    @Column(length = 150)
    private String titulaire;

    /**
     * Cumul des versements confirmés vers ce compte. C'est un indicateur de
     * suivi interne, pas le solde réel de la banque — la résidence n'a aucun
     * moyen de connaître les mouvements qu'elle n'a pas saisis.
     */
    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Identifiant lisible sur les reçus de versement, selon le type de compte. */
    public String getIdentifiant() {
        return type == TypeCompteFinancier.MOBILE_MONEY ? numeroTelephone : numeroCompte;
    }
}
