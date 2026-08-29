package com.gnimah.backend.controller;

import com.gnimah.backend.dto.caisse.*;
import com.gnimah.backend.service.CaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/caisse")
@RequiredArgsConstructor
@Tag(name = "Gestion de caisse")
public class CaisseController {

    private final CaisseService caisseService;

    // ---- Référentiel ----

    @GetMapping("/caisses")
    @Operation(summary = "Lister les caisses actives")
    public ResponseEntity<List<CaisseResponse>> findAll() {
        return ResponseEntity.ok(caisseService.findAll());
    }

    @GetMapping("/caisses/{id}")
    @Operation(summary = "Détail d'une caisse")
    public ResponseEntity<CaisseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(caisseService.getById(id));
    }

    @PostMapping("/caisses")
    @Operation(summary = "Créer une caisse")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<CaisseResponse> create(@Valid @RequestBody CaisseRequest request) {
        return ResponseEntity.ok(caisseService.create(request));
    }

    @PutMapping("/caisses/{id}")
    @Operation(summary = "Modifier une caisse")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<CaisseResponse> update(@PathVariable Long id, @Valid @RequestBody CaisseRequest request) {
        return ResponseEntity.ok(caisseService.update(id, request));
    }

    // ---- Sessions ----

    @PostMapping("/sessions/ouvrir")
    @Operation(summary = "Ouvrir une vacation de caisse")
    public ResponseEntity<SessionCaisseResponse> ouvrir(@Valid @RequestBody OuvertureCaisseRequest request) {
        return ResponseEntity.ok(caisseService.ouvrir(request));
    }

    @PostMapping("/sessions/{id}/fermer")
    @Operation(summary = "Fermer une vacation après comptage")
    public ResponseEntity<SessionCaisseResponse> fermer(@PathVariable Long id,
                                                        @Valid @RequestBody FermetureCaisseRequest request) {
        return ResponseEntity.ok(caisseService.fermer(id, request));
    }

    @GetMapping("/sessions/ouvertes")
    @Operation(summary = "Vacations actuellement ouvertes")
    public ResponseEntity<List<SessionCaisseResponse>> sessionsOuvertes() {
        return ResponseEntity.ok(caisseService.sessionsOuvertes());
    }

    @GetMapping("/caisses/{id}/session")
    @Operation(summary = "Vacation ouverte d'une caisse, s'il y en a une")
    public ResponseEntity<SessionCaisseResponse> sessionOuverte(@PathVariable Long id) {
        return ResponseEntity.ok(caisseService.sessionOuverte(id));
    }

    @GetMapping("/caisses/{id}/sessions")
    @Operation(summary = "Historique des vacations d'une caisse")
    public ResponseEntity<Page<SessionCaisseResponse>> historique(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(caisseService.historique(id, PageRequest.of(page, size)));
    }

    // ---- Mouvements ----

    @PostMapping("/mouvements")
    @Operation(summary = "Enregistrer un mouvement de caisse manuel")
    public ResponseEntity<MouvementCaisseResponse> enregistrer(@Valid @RequestBody MouvementCaisseRequest request) {
        return ResponseEntity.ok(caisseService.enregistrerManuel(request));
    }

    @GetMapping("/sessions/{id}/mouvements")
    @Operation(summary = "Mouvements d'une vacation")
    public ResponseEntity<List<MouvementCaisseResponse>> mouvementsSession(@PathVariable Long id) {
        return ResponseEntity.ok(caisseService.mouvementsSession(id));
    }

    @GetMapping("/caisses/{id}/mouvements")
    @Operation(summary = "Mouvements d'une caisse")
    public ResponseEntity<Page<MouvementCaisseResponse>> mouvementsCaisse(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(caisseService.mouvementsCaisse(id, PageRequest.of(page, size)));
    }

    @GetMapping("/journal")
    @Operation(summary = "Journal de caisse sur une période")
    public ResponseEntity<List<MouvementCaisseResponse>> journal(
            @RequestParam String debut,
            @RequestParam String fin) {
        return ResponseEntity.ok(caisseService.journal(
                LocalDate.parse(debut).atStartOfDay(),
                LocalDate.parse(fin).plusDays(1).atStartOfDay()));
    }
}
