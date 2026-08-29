package com.gnimah.backend.dto.versement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VersementResponse {
    private Long id;
    private String numero;
    private Long caisseId;
    private String caisseNom;
    private Long compteId;
    private String compteLibelle;
    private String compteType;
    private String compteIdentifiant;
    private String periodicite;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    private BigDecimal montant;
    private LocalDate dateVersement;
    private LocalTime heureVersement;
    private String mode;
    private String referenceTransaction;
    private String statut;
    private String caissierNom;
    private String valideParNom;
    private LocalDateTime dateValidation;
    private String justificatifUrl;
    private String notes;
    private LocalDateTime createdAt;
    private List<ActionVersementResponse> actions;
}
