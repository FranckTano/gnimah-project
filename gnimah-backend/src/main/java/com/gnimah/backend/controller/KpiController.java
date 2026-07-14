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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/kpi")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
@Tag(name = "KPI & Tableaux de bord")
public class KpiController {

    private final KpiService kpiService;

    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord KPI")
    public ResponseEntity<KpiResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        if (debut == null) debut = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        if (fin == null) fin = LocalDateTime.now();
        return ResponseEntity.ok(kpiService.getDashboard(debut, fin));
    }

    @GetMapping("/dashboard/today")
    @Operation(summary = "KPI du jour")
    public ResponseEntity<KpiResponse> getDashboardToday() {
        LocalDateTime debut = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        return ResponseEntity.ok(kpiService.getDashboard(debut, fin));
    }
}
