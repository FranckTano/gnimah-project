package com.gnimah.backend.dto.sejour;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Révision du prix d'un séjour déjà enregistré : nouveau tarif négocié et/ou
 * remise commerciale. Sert à corriger une note en cours, geste courant quand un
 * client prolonge ou qu'un arrangement est trouvé après l'arrivée.
 */
@Data
public class RemiseSejourRequest {

    /** Nouveau tarif unitaire ; laissé vide, le tarif en vigueur est conservé. */
    private BigDecimal tarifPersonnalise;

    /** POURCENTAGE ou MONTANT. Vide ou zéro : la remise est supprimée. */
    private String remiseType;
    private BigDecimal remiseValeur;
    private String remiseMotif;
}
