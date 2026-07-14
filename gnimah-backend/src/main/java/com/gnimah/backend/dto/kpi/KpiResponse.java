package com.gnimah.backend.dto.kpi;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class KpiResponse {
    // Financiers
    private BigDecimal chiffreAffaires;
    private BigDecimal encaissements;
    private BigDecimal resteAPayer;
    private BigDecimal depensesTotales;
    private BigDecimal marge;
    private BigDecimal panierMoyen;

    // Opérationnels
    private double tauxOccupation;
    private BigDecimal adr;
    private BigDecimal revPar;
    private BigDecimal tRevPar;
    private double alos;

    // Chambres
    private long totalChambres;
    private long chambresLibres;
    private long chambresOccupees;
    private long chambresANettoyer;
    private long chambresEnMaintenance;

    // Clients & Réservations
    private long totalSejours;
    private long nouveauxClients;
    private long clientsFideles;
    private long reservationsTotal;
    private long reservationsAnnulees;
    private long reservationsNoShow;
    private double tauxConversion;

    // Tendances
    private List<Map<String, Object>> caParJour;
    private List<Map<String, Object>> sejoursParType;
    private List<Map<String, Object>> performanceAgents;
    private List<Map<String, Object>> occupationParChambre;
}
