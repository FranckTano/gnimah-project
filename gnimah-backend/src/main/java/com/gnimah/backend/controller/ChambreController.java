package com.gnimah.backend.controller;

import com.gnimah.backend.dto.chambre.ChambreRequest;
import com.gnimah.backend.dto.chambre.ChambreResponse;
import com.gnimah.backend.service.ChambreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chambres")
@RequiredArgsConstructor
@Tag(name = "Gestion des chambres")
public class ChambreController {

    private final ChambreService chambreService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    @Operation(summary = "Créer une chambre")
    public ResponseEntity<ChambreResponse> create(@Valid @RequestBody ChambreRequest request) {
        return ResponseEntity.ok(chambreService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    @Operation(summary = "Modifier une chambre")
    public ResponseEntity<ChambreResponse> update(@PathVariable Long id, @Valid @RequestBody ChambreRequest request) {
        return ResponseEntity.ok(chambreService.update(id, request));
    }

    @PatchMapping("/{id}/etat")
    @Operation(summary = "Changer l'état d'une chambre")
    public ResponseEntity<ChambreResponse> updateEtat(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(chambreService.updateEtat(id, body.get("etat")));
    }

    @PatchMapping("/{id}/actif")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    @Operation(summary = "Activer / désactiver une chambre (suppression logique)")
    public ResponseEntity<ChambreResponse> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(chambreService.toggleActif(id));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les chambres actives")
    public ResponseEntity<List<ChambreResponse>> findAll() {
        return ResponseEntity.ok(chambreService.findAll());
    }

    @GetMapping("/toutes")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN','RESPONSABLE')")
    @Operation(summary = "Lister toutes les chambres, y compris désactivées (écran de gestion)")
    public ResponseEntity<List<ChambreResponse>> findAllAdmin() {
        return ResponseEntity.ok(chambreService.findAllAdmin());
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Chambres disponibles (libres)")
    public ResponseEntity<List<ChambreResponse>> findDisponibles() {
        return ResponseEntity.ok(chambreService.findDisponibles());
    }

    @GetMapping("/disponibles-periode")
    @Operation(summary = "Chambres disponibles sur une période")
    public ResponseEntity<List<ChambreResponse>> findDisponiblesPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        return ResponseEntity.ok(chambreService.findDisponiblesPeriode(dateDebut, dateFin));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une chambre par ID")
    public ResponseEntity<ChambreResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chambreService.getById(id));
    }
}
