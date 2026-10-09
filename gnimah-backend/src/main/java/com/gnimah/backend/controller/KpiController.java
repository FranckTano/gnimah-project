package com.gnimah.backend.controller;

import com.gnimah.backend.dto.kpi.KpiResponse;
import com.gnimah.backend.service.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/kpi")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
@Tag(name = "KPI & Tableaux de bord")
public class KpiController {

    private final KpiService kpiService;

    /**
     * {@code fin} est inclusive pour l'appelant (l'écran envoie « jour à 23:59:59 ») ;
     * elle est convertie ici en borne exclue, comme l'attendent les requêtes.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord KPI")
    public ResponseEntity<KpiResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finExclue = fin != null
                ? fin.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1)
                : LocalDate.now().plusDays(1).atStartOfDay();
        return ResponseEntity.ok(kpiService.getDashboard(debut, finExclue));
    }

    @GetMapping("/dashboard/today")
    @Operation(summary = "KPI du jour")
    public ResponseEntity<KpiResponse> getDashboardToday() {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        return ResponseEntity.ok(kpiService.getDashboard(debut, debut.plusDays(1)));
    }
}
