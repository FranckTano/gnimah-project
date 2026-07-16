package com.gnimah.backend.controller;

import com.gnimah.backend.dto.auth.PasswordResetRequestResponse;
import com.gnimah.backend.dto.auth.ResoudreReinitialisationResponse;
import com.gnimah.backend.service.PasswordResetRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/password-reset-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Demandes de réinitialisation de mot de passe")
public class PasswordResetRequestController {

    private final PasswordResetRequestService passwordResetRequestService;

    @GetMapping
    @Operation(summary = "Lister les demandes de réinitialisation en attente")
    public ResponseEntity<List<PasswordResetRequestResponse>> findEnAttente() {
        return ResponseEntity.ok(passwordResetRequestService.findEnAttente());
    }

    @PostMapping("/{id}/resoudre")
    @Operation(summary = "Générer un nouveau mot de passe temporaire pour la demande")
    public ResponseEntity<ResoudreReinitialisationResponse> resoudre(@PathVariable Long id) {
        return ResponseEntity.ok(passwordResetRequestService.resoudre(id));
    }
}
