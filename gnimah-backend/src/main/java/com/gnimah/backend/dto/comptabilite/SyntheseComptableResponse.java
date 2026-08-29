package com.gnimah.backend.dto.comptabilite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyntheseComptableResponse {
    private LocalDate debut;
    private LocalDate fin;
    // --- Recettes ---
    private BigDecimal recetteHebergement;
    private BigDecimal recetteRestauration;
    private BigDecimal recetteEvenements;
    private BigDecimal autresRecettes;
    private BigDecimal totalRecettes;
    // --- Charges ---
    private BigDecimal totalDepenses;
    private BigDecimal depensesPayees;
    private BigDecimal depensesEnAttente;
    // --- Resultat ---
    private BigDecimal resultat;
    private double tauxMarge;
    // --- Tresorerie ---
    private BigDecimal soldeCaisses;
    private BigDecimal entreesCaisse;
    private BigDecimal sortiesCaisse;
    private BigDecimal totalVerse;
    private BigDecimal resteAVerser;
    // --- Creances et stock ---
    private BigDecimal creancesClients;
    private BigDecimal valeurStock;
    private BigDecimal coutIncidents;
    // --- Details ---
    private List<LigneRepartitionResponse> depensesParFamille;
    private List<LigneRepartitionResponse> recettesParSource;
    private List<LigneRepartitionResponse> topFournisseurs;
    private List<Map<String, Object>> evolutionJournaliere;
}
