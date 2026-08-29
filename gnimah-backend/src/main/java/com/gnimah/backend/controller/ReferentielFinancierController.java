package com.gnimah.backend.controller;

import com.gnimah.backend.dto.compte.CompteFinancierRequest;
import com.gnimah.backend.dto.compte.CompteFinancierResponse;
import com.gnimah.backend.dto.fournisseur.FournisseurRequest;
import com.gnimah.backend.dto.fournisseur.FournisseurResponse;
import com.gnimah.backend.service.CompteFinancierService;
import com.gnimah.backend.service.FournisseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Référentiels partagés par la comptabilité et la caisse : fournisseurs et
 * comptes de versement. Regroupés dans un même contrôleur car ce sont deux
 * listes de paramétrage consultées partout et modifiées rarement.
 */
@RestController
@RequestMapping("/referentiel")
@RequiredArgsConstructor
@Tag(name = "Référentiels financiers")
public class ReferentielFinancierController {

    private final FournisseurService fournisseurService;
    private final CompteFinancierService compteService;

    // ---- Fournisseurs ----

    @GetMapping("/fournisseurs")
    @Operation(summary = "Lister les fournisseurs actifs")
    public ResponseEntity<List<FournisseurResponse>> fournisseurs(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(type != null && !type.isBlank()
                ? fournisseurService.findByType(type)
                : fournisseurService.findAll());
    }

    @PostMapping("/fournisseurs")
    @Operation(summary = "Créer un fournisseur")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<FournisseurResponse> creerFournisseur(@Valid @RequestBody FournisseurRequest request) {
        return ResponseEntity.ok(fournisseurService.create(request));
    }

    @PutMapping("/fournisseurs/{id}")
    @Operation(summary = "Modifier un fournisseur")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<FournisseurResponse> modifierFournisseur(@PathVariable Long id,
                                                                   @Valid @RequestBody FournisseurRequest request) {
        return ResponseEntity.ok(fournisseurService.update(id, request));
    }

    @DeleteMapping("/fournisseurs/{id}")
    @Operation(summary = "Désactiver un fournisseur")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<Void> desactiverFournisseur(@PathVariable Long id) {
        fournisseurService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Comptes de versement ----

    @GetMapping("/comptes")
    @Operation(summary = "Lister les comptes bancaires et mobile money")
    public ResponseEntity<List<CompteFinancierResponse>> comptes() {
        return ResponseEntity.ok(compteService.findAll());
    }

    @PostMapping("/comptes")
    @Operation(summary = "Créer un compte de versement")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<CompteFinancierResponse> creerCompte(@Valid @RequestBody CompteFinancierRequest request) {
        return ResponseEntity.ok(compteService.create(request));
    }

    @PutMapping("/comptes/{id}")
    @Operation(summary = "Modifier un compte de versement")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<CompteFinancierResponse> modifierCompte(@PathVariable Long id,
                                                                  @Valid @RequestBody CompteFinancierRequest request) {
        return ResponseEntity.ok(compteService.update(id, request));
    }

    @DeleteMapping("/comptes/{id}")
    @Operation(summary = "Désactiver un compte de versement")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<Void> desactiverCompte(@PathVariable Long id) {
        compteService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}
