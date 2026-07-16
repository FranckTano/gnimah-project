package com.gnimah.backend.controller;

import com.gnimah.backend.dto.entretien.TacheEntretienRequest;
import com.gnimah.backend.dto.entretien.TacheEntretienResponse;
import com.gnimah.backend.entity.enums.StatutTache;
import com.gnimah.backend.service.TacheEntretienService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entretien")
@RequiredArgsConstructor
@Tag(name = "Entretien / Housekeeping")
public class TacheEntretienController {

    private final TacheEntretienService tacheEntretienService;

    @GetMapping
    @Operation(summary = "Lister les tâches d'entretien")
    public ResponseEntity<List<TacheEntretienResponse>> findAll(@RequestParam(required = false) String statut) {
        StatutTache statutTache = (statut != null && !statut.isBlank()) ? StatutTache.valueOf(statut) : null;
        return ResponseEntity.ok(tacheEntretienService.findAll(statutTache));
    }

    @GetMapping("/en-attente")
    @Operation(summary = "Lister les tâches à faire")
    public ResponseEntity<List<TacheEntretienResponse>> findEnAttente() {
        return ResponseEntity.ok(tacheEntretienService.findEnAttente());
    }

    @GetMapping("/chambre/{chambreId}")
    @Operation(summary = "Lister les tâches d'une chambre")
    public ResponseEntity<List<TacheEntretienResponse>> findByChambre(@PathVariable Long chambreId) {
        return ResponseEntity.ok(tacheEntretienService.findByChambre(chambreId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    @Operation(summary = "Créer une tâche d'entretien (Responsable, Directeur ou Admin uniquement)")
    public ResponseEntity<TacheEntretienResponse> create(@Valid @RequestBody TacheEntretienRequest request) {
        return ResponseEntity.ok(tacheEntretienService.create(request));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'une tâche")
    public ResponseEntity<TacheEntretienResponse> updateStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tacheEntretienService.updateStatut(id, StatutTache.valueOf(body.get("statut"))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une tâche d'entretien")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tacheEntretienService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
