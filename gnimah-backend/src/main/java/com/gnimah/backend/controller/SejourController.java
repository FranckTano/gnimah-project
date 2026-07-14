package com.gnimah.backend.controller;

import com.gnimah.backend.dto.sejour.SejourRequest;
import com.gnimah.backend.dto.sejour.SejourResponse;
import com.gnimah.backend.service.SejourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sejours")
@RequiredArgsConstructor
@Tag(name = "Gestion des séjours")
public class SejourController {

    private final SejourService sejourService;

    @PostMapping("/check-in")
    @Operation(summary = "Enregistrer un séjour (check-in)")
    public ResponseEntity<SejourResponse> checkIn(@Valid @RequestBody SejourRequest request) {
        return ResponseEntity.ok(sejourService.checkIn(request));
    }

    @PostMapping("/{id}/check-out")
    @Operation(summary = "Clôturer un séjour (check-out)")
    public ResponseEntity<SejourResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(sejourService.checkOut(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un séjour par ID")
    public ResponseEntity<SejourResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sejourService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister tous les séjours")
    public ResponseEntity<Page<SejourResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sejourService.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/en-cours")
    @Operation(summary = "Séjours en cours")
    public ResponseEntity<List<SejourResponse>> findEnCours() {
        return ResponseEntity.ok(sejourService.findSejoursEnCours());
    }

    @GetMapping("/departs-jour")
    @Operation(summary = "Départs du jour")
    public ResponseEntity<List<SejourResponse>> findDepartsJour() {
        return ResponseEntity.ok(sejourService.findDepartsJour());
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Historique séjours d'un client")
    public ResponseEntity<Page<SejourResponse>> findByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sejourService.findByClient(clientId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateEntree"))));
    }
}
