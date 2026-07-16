package com.gnimah.backend.controller;

import com.gnimah.backend.dto.utilisateur.UtilisateurRequest;
import com.gnimah.backend.dto.utilisateur.UtilisateurResponse;
import com.gnimah.backend.service.UtilisateurService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Gestion des utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<List<UtilisateurResponse>> findAll() {
        return ResponseEntity.ok(utilisateurService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
    public ResponseEntity<UtilisateurResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UtilisateurResponse> create(@Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurResponse> update(@PathVariable Long id, @Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.update(id, request));
    }

    @PatchMapping("/{id}/toggle-actif")
    public ResponseEntity<UtilisateurResponse> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.toggleActif(id));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        utilisateurService.resetPassword(id, body.get("newPassword"));
        return ResponseEntity.ok().build();
    }
}
