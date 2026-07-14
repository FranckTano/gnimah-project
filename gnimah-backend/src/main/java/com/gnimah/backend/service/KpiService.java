package com.gnimah.backend.service;

import com.gnimah.backend.dto.kpi.KpiResponse;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final SejourRepository sejourRepository;
    private final ChambreRepository chambreRepository;
    private final ClientRepository clientRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final DepenseRepository depenseRepository;

    @Transactional(readOnly = true)
    public KpiResponse getDashboard(LocalDateTime debut, LocalDateTime fin) {
        BigDecimal chiffreAffaires = sejourRepository.sumChiffreAffaires(debut, fin);
        BigDecimal encaissements = sejourRepository.sumEncaissements(debut, fin);
        BigDecimal depenses = depenseRepository.sumDepensesPeriode(debut.toLocalDate(), fin.toLocalDate());
        BigDecimal marge = chiffreAffaires.subtract(depenses);

        long totalSejours = sejourRepository.countSejoursPeriode(debut, fin);
        BigDecimal panierMoyen = totalSejours > 0
                ? chiffreAffaires.divide(BigDecimal.valueOf(totalSejours), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalChambres = chambreRepository.countByActifTrue();
        long chambresOccupees = chambreRepository.countByEtat(EtatChambre.OCCUPEE);
        long chambresLibres = chambreRepository.countByEtat(EtatChambre.LIBRE);
        long chambresANettoyer = chambreRepository.countByEtat(EtatChambre.A_NETTOYER);
        long chambresEnMaintenance = chambreRepository.countByEtat(EtatChambre.EN_MAINTENANCE);

        double tauxOccupation = totalChambres > 0
                ? (double) chambresOccupees / totalChambres * 100 : 0;

        BigDecimal adr = chambresOccupees > 0
                ? chiffreAffaires.divide(BigDecimal.valueOf(chambresOccupees), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal revPar = totalChambres > 0
                ? adr.multiply(BigDecimal.valueOf(tauxOccupation / 100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Double avgDuree = sejourRepository.avgDureeSejour(debut, fin);
        double alos = avgDuree != null ? avgDuree : 0;

        long clientsFideles = clientRepository.countByNbSejoursGreaterThanEqual(3);
        long reservationsTotal = reservationRepository.countReservationsPeriode(debut, fin);
        long annulees = reservationRepository.countByStatut(StatutReservation.ANNULEE);
        long noShow = reservationRepository.countByStatut(StatutReservation.NO_SHOW);

        double tauxConversion = reservationsTotal > 0
                ? (double) totalSejours / reservationsTotal * 100 : 0;

        return KpiResponse.builder()
                .chiffreAffaires(chiffreAffaires)
                .encaissements(encaissements)
                .resteAPayer(chiffreAffaires.subtract(encaissements))
                .depensesTotales(depenses)
                .marge(marge)
                .panierMoyen(panierMoyen)
                .tauxOccupation(Math.round(tauxOccupation * 10.0) / 10.0)
                .adr(adr)
                .revPar(revPar)
                .tRevPar(revPar)
                .alos(Math.round(alos * 10.0) / 10.0)
                .totalChambres(totalChambres)
                .chambresLibres(chambresLibres)
                .chambresOccupees(chambresOccupees)
                .chambresANettoyer(chambresANettoyer)
                .chambresEnMaintenance(chambresEnMaintenance)
                .totalSejours(totalSejours)
                .nouveauxClients(sejourRepository.countClientsUniques(debut, fin))
                .clientsFideles(clientsFideles)
                .reservationsTotal(reservationsTotal)
                .reservationsAnnulees(annulees)
                .reservationsNoShow(noShow)
                .tauxConversion(Math.round(tauxConversion * 10.0) / 10.0)
                .caParJour(buildCaParJour(debut, fin))
                .sejoursParType(buildSejoursParType())
                .performanceAgents(buildPerformanceAgents(debut, fin))
                .occupationParChambre(buildOccupationParChambre())
                .build();
    }

    private List<Map<String, Object>> buildCaParJour(LocalDateTime debut, LocalDateTime fin) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime current = debut.toLocalDate().atStartOfDay();
        LocalDateTime end = fin.toLocalDate().atStartOfDay();
        while (!current.isAfter(end)) {
            LocalDateTime nextDay = current.plusDays(1);
            BigDecimal ca = sejourRepository.sumChiffreAffaires(current, nextDay);
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", current.toLocalDate().toString());
            entry.put("ca", ca);
            result.add(entry);
            current = nextDay;
        }
        return result;
    }

    private List<Map<String, Object>> buildSejoursParType() {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> passage = new HashMap<>();
        passage.put("type", "PASSAGE");
        passage.put("count", 0);
        Map<String, Object> sejour = new HashMap<>();
        sejour.put("type", "SEJOUR");
        sejour.put("count", 0);
        result.add(passage);
        result.add(sejour);
        return result;
    }

    private List<Map<String, Object>> buildPerformanceAgents(LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> raw = sejourRepository.findPerformanceAgents(debut, fin);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("agentId", row[0]);
            entry.put("nbSejours", row[1]);
            entry.put("ca", row[2]);
            result.add(entry);
        }
        return result;
    }

    private List<Map<String, Object>> buildOccupationParChambre() {
        return new ArrayList<>();
    }
}
