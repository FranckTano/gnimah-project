package com.gnimah.backend.controller;

import com.gnimah.backend.dto.incident.IncidentRequest;
import com.gnimah.backend.dto.incident.IncidentResponse;
import com.gnimah.backend.dto.incident.ResolutionIncidentRequest;
import com.gnimah.backend.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Objets et matériels défectueux relevés en chambre par le service d'entretien.
 *
 * La déclaration est ouverte à tout agent — c'est la femme de chambre qui
 * constate. La résolution, qui engage de l'argent, est réservée à l'encadrement.
 */
@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents chambre")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @Operation(summary = "Lister les incidents")
    public ResponseEntity<List<IncidentResponse>> findAll() {
        return ResponseEntity.ok(incidentService.findAll());
    }

    @GetMapping("/ouverts")
    @Operation(summary = "Incidents encore à traiter")
    public ResponseEntity<List<IncidentResponse>> findOuverts() {
        return ResponseEntity.ok(incidentService.findOuverts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un incident")
    public ResponseEntity<IncidentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getById(id));
    }

    @GetMapping("/chambre/{chambreId}")
    @Operation(summary = "Incidents d'une chambre")
    public ResponseEntity<List<IncidentResponse>> findByChambre(@PathVariable Long chambreId) {
        return ResponseEntity.ok(incidentService.findByChambre(chambreId));
    }

    @PostMapping
    @Operation(summary = "Déclarer un objet ou matériel défectueux")
    public ResponseEntity<IncidentResponse> declarer(@Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.ok(incidentService.declarer(request));
    }

    @PostMapping("/{id}/prise-en-charge")
    @Operation(summary = "Prendre l'incident en charge")
    public ResponseEntity<IncidentResponse> prendreEnCharge(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.prendreEnCharge(id));
    }

    @PostMapping("/{id}/resolution")
    @Operation(summary = "Clôturer l'incident — génère la charge ou facture le client")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<IncidentResponse> resoudre(@PathVariable Long id,
                                                     @Valid @RequestBody ResolutionIncidentRequest request) {
        return ResponseEntity.ok(incidentService.resoudre(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un incident sans conséquence financière")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        incidentService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
