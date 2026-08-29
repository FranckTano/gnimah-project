package com.gnimah.backend.controller;

import com.gnimah.backend.dto.comptabilite.SyntheseComptableResponse;
import com.gnimah.backend.service.ComptabiliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/comptabilite")
@RequiredArgsConstructor
@Tag(name = "Comptabilité")
public class ComptabiliteController {

    private final ComptabiliteService comptabiliteService;

    @GetMapping("/synthese")
    @Operation(summary = "Compte de résultat simplifié et position de trésorerie")
    public ResponseEntity<SyntheseComptableResponse> synthese(
            @RequestParam(required = false) String debut,
            @RequestParam(required = false) String fin) {
        LocalDate fnl = fin != null ? LocalDate.parse(fin) : LocalDate.now();
        LocalDate dbt = debut != null ? LocalDate.parse(debut) : fnl.withDayOfMonth(1);
        return ResponseEntity.ok(comptabiliteService.synthese(dbt, fnl));
    }
}
