package com.gnimah.backend.controller;

import com.gnimah.backend.dto.evenement.EvenementRequest;
import com.gnimah.backend.dto.evenement.EvenementResponse;
import com.gnimah.backend.entity.enums.StatutEvenement;
import com.gnimah.backend.service.EvenementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evenements")
@RequiredArgsConstructor
@Tag(name = "Gestion des événements")
public class EvenementController {

    private final EvenementService evenementService;

    @PostMapping
    @Operation(summary = "Créer un événement")
    public ResponseEntity<EvenementResponse> create(@Valid @RequestBody EvenementRequest request) {
        return ResponseEntity.ok(evenementService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un événement")
    public ResponseEntity<EvenementResponse> update(@PathVariable Long id, @Valid @RequestBody EvenementRequest request) {
        return ResponseEntity.ok(evenementService.update(id, request));
    }

    @GetMapping
    @Operation(summary = "Lister tous les événements")
    public ResponseEntity<List<EvenementResponse>> findAll() {
        return ResponseEntity.ok(evenementService.findAll());
    }

    @GetMapping("/calendrier")
    @Operation(summary = "Lister les événements d'un mois")
    public ResponseEntity<List<EvenementResponse>> findByMois(
            @RequestParam int annee,
            @RequestParam int mois) {
        return ResponseEntity.ok(evenementService.findByMois(annee, mois));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'un événement")
    public ResponseEntity<EvenementResponse> updateStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(evenementService.updateStatut(id, StatutEvenement.valueOf(body.get("statut"))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un événement")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        evenementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
