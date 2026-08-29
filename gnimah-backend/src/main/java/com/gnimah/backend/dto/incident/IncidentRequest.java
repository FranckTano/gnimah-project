package com.gnimah.backend.dto.incident;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class IncidentRequest {
    @NotNull(message = "La chambre est requise")
    private Long chambreId;
    /** Sejour du dernier occupant, pour pouvoir imputer le degat au client. */
    private Long sejourId;
    @NotNull(message = "Le type d incident est requis")
    private String type;
    @NotBlank(message = "La designation de l objet ou du materiel est requise")
    private String designation;
    private Long articleStockId;
    private Integer quantite;
    private String gravite;
    private String description;
    private String photo;
    private BigDecimal coutEstime;
    private Boolean imputeClient;
    /** Met la chambre en maintenance et ouvre une tache pour l equipe technique. */
    private Boolean bloquerChambre;
    private String notes;
}
