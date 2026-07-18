package com.gnimah.backend.controller;

import com.gnimah.backend.dto.paiement.PaiementRequest;
import com.gnimah.backend.dto.paiement.PaiementResponse;
import com.gnimah.backend.service.PaiementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paiements")
@RequiredArgsConstructor
@Tag(name = "Gestion des paiements")
public class PaiementController {

    private final PaiementService paiementService;

    @GetMapping
    @Operation(summary = "Liste paginée des paiements")
    public ResponseEntity<Page<PaiementResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paiementService.findAll(PageRequest.of(page, size, Sort.by("datePaiement").descending())));
    }

    @PostMapping
    @Operation(summary = "Enregistrer un paiement")
    public ResponseEntity<PaiementResponse> enregistrer(@Valid @RequestBody PaiementRequest request) {
        return ResponseEntity.ok(paiementService.enregistrer(request));
    }

    @GetMapping("/sejour/{sejourId}")
    @Operation(summary = "Paiements d'un séjour")
    public ResponseEntity<List<PaiementResponse>> findBySejour(@PathVariable Long sejourId) {
        return ResponseEntity.ok(paiementService.findBySejour(sejourId));
    }

    @GetMapping("/{id}/recu")
    @Operation(summary = "Télécharger le reçu PDF d'un paiement")
    public ResponseEntity<byte[]> telechargerRecu(@PathVariable Long id) {
        byte[] pdf = paiementService.generateRecuPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recu-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
