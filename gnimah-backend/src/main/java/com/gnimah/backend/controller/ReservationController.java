package com.gnimah.backend.controller;

import com.gnimah.backend.dto.reservation.ReservationRequest;
import com.gnimah.backend.dto.reservation.ReservationResponse;
import com.gnimah.backend.service.ReservationService;
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
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Gestion des réservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Créer une réservation")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.create(request));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'une réservation")
    public ResponseEntity<ReservationResponse> updateStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reservationService.updateStatut(id, body.get("statut")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une réservation")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les réservations")
    public ResponseEntity<Page<ReservationResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reservationService.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateArrivee"))));
    }

    @GetMapping("/arrivees-jour")
    @Operation(summary = "Arrivées du jour")
    public ResponseEntity<List<ReservationResponse>> findArriveesDuJour() {
        return ResponseEntity.ok(reservationService.findArriveesDuJour());
    }
}
