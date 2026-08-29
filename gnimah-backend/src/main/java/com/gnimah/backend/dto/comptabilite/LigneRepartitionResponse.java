package com.gnimah.backend.dto.comptabilite;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LigneRepartitionResponse {
    private String cle;
    private String libelle;
    private long nombre;
    private BigDecimal montant;
    /** Part du poste dans le total, en pourcentage arrondi a 0,1. */
    private double part;
}
