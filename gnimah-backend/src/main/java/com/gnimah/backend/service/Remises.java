package com.gnimah.backend.service;

import com.gnimah.backend.entity.enums.TypeRemise;
import com.gnimah.backend.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcul des remises commerciales, partagé par les séjours, les réservations et
 * les additions du restaurant.
 *
 * La règle est la même partout : une remise ne peut jamais dépasser le montant
 * brut. Une remise supérieure produirait un montant à payer négatif, donc une
 * dette de la résidence envers le client — ce que l'application n'a aucun moyen
 * de traiter et qui traduit en pratique une faute de saisie.
 */
public final class Remises {

    private Remises() {}

    /** Plafond de remise qu'un agent de réception peut accorder seul. */
    public static final BigDecimal PLAFOND_AGENT_POURCENT = BigDecimal.valueOf(20);

    /**
     * Convertit la remise saisie en montant ferme, en francs.
     *
     * @param brut   montant avant remise
     * @param type   POURCENTAGE ou MONTANT ; {@code null} signifie aucune remise
     * @param valeur pourcentage (ex. 15) ou montant ferme selon le type
     */
    public static BigDecimal calculer(BigDecimal brut, TypeRemise type, BigDecimal valeur) {
        if (type == null || valeur == null || valeur.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (brut == null || brut.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal remise;
        if (type == TypeRemise.POURCENTAGE) {
            if (valeur.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Une remise en pourcentage ne peut pas dépasser 100 %");
            }
            remise = brut.multiply(valeur)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            remise = valeur.setScale(0, RoundingMode.HALF_UP);
        }

        if (remise.compareTo(brut) > 0) {
            throw new BusinessException("La remise (" + remise + " FCFA) dépasse le montant à payer ("
                    + brut.setScale(0, RoundingMode.HALF_UP) + " FCFA)");
        }
        return remise;
    }

    /** Part de la remise dans le brut, en pourcentage — sert aux contrôles d'habilitation. */
    public static BigDecimal pourcentage(BigDecimal brut, BigDecimal remise) {
        if (brut == null || brut.compareTo(BigDecimal.ZERO) <= 0 || remise == null) {
            return BigDecimal.ZERO;
        }
        return remise.multiply(BigDecimal.valueOf(100))
                .divide(brut, 1, RoundingMode.HALF_UP);
    }

    /** Type de remise à partir de sa forme texte ; {@code null} si rien n'est saisi. */
    public static TypeRemise parse(String value) {
        if (value == null || value.isBlank()) return null;
        return CaisseService.parseEnum(TypeRemise.class, value, "type de remise");
    }
}
