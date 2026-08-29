package com.gnimah.backend.controller;

import com.gnimah.backend.dto.depense.DepenseRequest;
import com.gnimah.backend.dto.depense.DepenseResponse;
import com.gnimah.backend.service.DepenseService;
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
import java.util.Map;

@RestController
@RequestMapping("/depenses")
@RequiredArgsConstructor
@Tag(name = "Gestion des dépenses")
public class DepenseController {

    private final DepenseService depenseService;

    @GetMapping
    @Operation(summary = "Lister les dépenses")
    public ResponseEntity<Page<DepenseResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(depenseService.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une dépense")
    public ResponseEntity<DepenseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(depenseService.getById(id));
    }

    @GetMapping("/periode")
    @Operation(summary = "Dépenses d'une période")
    public ResponseEntity<List<DepenseResponse>> findByPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        return ResponseEntity.ok(depenseService.findPeriode(LocalDate.parse(debut), LocalDate.parse(fin)));
    }

    @GetMapping("/en-attente")
    @Operation(summary = "Dépenses en attente de validation")
    public ResponseEntity<List<DepenseResponse>> findEnAttente() {
        return ResponseEntity.ok(depenseService.findEnAttente());
    }

    @PostMapping
    @Operation(summary = "Enregistrer une dépense")
    public ResponseEntity<DepenseResponse> create(@Valid @RequestBody DepenseRequest request) {
        return ResponseEntity.ok(depenseService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une dépense non réglée")
    public ResponseEntity<DepenseResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody DepenseRequest request) {
        return ResponseEntity.ok(depenseService.update(id, request));
    }

    @PostMapping("/{id}/validation")
    @Operation(summary = "Valider une dépense")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<DepenseResponse> valider(@PathVariable Long id) {
        return ResponseEntity.ok(depenseService.valider(id));
    }

    @PostMapping("/{id}/rejet")
    @Operation(summary = "Rejeter une dépense")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<DepenseResponse> rejeter(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(depenseService.rejeter(id, body != null ? body.get("motif") : null));
    }

    @PostMapping("/{id}/reglement")
    @Operation(summary = "Régler une dépense — décaisse la caisse")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<DepenseResponse> payer(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        Long caisseId = body != null && body.get("caisseId") != null
                ? Long.valueOf(body.get("caisseId").toString()) : null;
        return ResponseEntity.ok(depenseService.payer(id, caisseId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une dépense non réglée")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        depenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
