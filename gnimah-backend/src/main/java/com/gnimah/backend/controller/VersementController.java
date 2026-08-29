package com.gnimah.backend.controller;

import com.gnimah.backend.dto.versement.ActionVersementRequest;
import com.gnimah.backend.dto.versement.RecetteAVerserResponse;
import com.gnimah.backend.dto.versement.VersementRequest;
import com.gnimah.backend.dto.versement.VersementResponse;
import com.gnimah.backend.service.VersementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/versements")
@RequiredArgsConstructor
@Tag(name = "Versements de recette")
public class VersementController {

    private final VersementService versementService;

    @GetMapping
    @Operation(summary = "Lister les versements")
    public ResponseEntity<Page<VersementResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(versementService.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateVersement", "id"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un versement et journal de ses actions")
    public ResponseEntity<VersementResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(versementService.getById(id));
    }

    @GetMapping("/periode")
    @Operation(summary = "Versements d'une période")
    public ResponseEntity<List<VersementResponse>> findPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        return ResponseEntity.ok(versementService.findPeriode(LocalDate.parse(debut), LocalDate.parse(fin)));
    }

    @GetMapping("/recette-a-verser")
    @Operation(summary = "Recette encaissée non encore reversée, pour préparer un versement")
    public ResponseEntity<RecetteAVerserResponse> recetteAVerser(
            @RequestParam(required = false) Long caisseId,
            @RequestParam String debut,
            @RequestParam String fin) {
        return ResponseEntity.ok(versementService.recetteAVerser(
                caisseId, LocalDate.parse(debut), LocalDate.parse(fin)));
    }

    @PostMapping
    @Operation(summary = "Préparer un versement")
    public ResponseEntity<VersementResponse> preparer(@Valid @RequestBody VersementRequest request) {
        return ResponseEntity.ok(versementService.preparer(request));
    }

    @PostMapping("/{id}/depot")
    @Operation(summary = "Enregistrer le dépôt effectif — sort l'argent de la caisse")
    public ResponseEntity<VersementResponse> deposer(@PathVariable Long id,
                                                     @RequestBody(required = false) ActionVersementRequest request) {
        return ResponseEntity.ok(versementService.deposer(id, request));
    }

    @PostMapping("/{id}/confirmation")
    @Operation(summary = "Confirmer le versement au vu du justificatif")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<VersementResponse> confirmer(@PathVariable Long id,
                                                       @RequestBody(required = false) ActionVersementRequest request) {
        return ResponseEntity.ok(versementService.confirmer(id, request));
    }

    @PostMapping("/{id}/annulation")
    @Operation(summary = "Annuler un versement")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<VersementResponse> annuler(@PathVariable Long id,
                                                     @RequestBody(required = false) ActionVersementRequest request) {
        return ResponseEntity.ok(versementService.annuler(id, request));
    }
}
