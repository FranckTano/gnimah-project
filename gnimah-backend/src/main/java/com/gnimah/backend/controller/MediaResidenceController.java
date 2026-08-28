package com.gnimah.backend.controller;

import com.gnimah.backend.dto.media.MediaResidenceRequest;
import com.gnimah.backend.dto.media.MediaResidenceResponse;
import com.gnimah.backend.service.MediaResidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Galerie de la résidence.
 *
 * La lecture est ouverte à tout utilisateur connecté — les visuels servent de
 * repère à l'équipe de réception. Toute écriture est réservée à l'administration.
 */
@RestController
@RequestMapping("/medias")
@RequiredArgsConstructor
@Tag(name = "Galerie de la résidence")
public class MediaResidenceController {

    private final MediaResidenceService service;

    @GetMapping
    @Operation(summary = "Galerie visible (visuels actifs)")
    public ResponseEntity<List<MediaResidenceResponse>> findVisibles() {
        return ResponseEntity.ok(service.findVisibles());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Galerie complète, visuels masqués compris")
    public ResponseEntity<List<MediaResidenceResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /** Le frontend masque l'envoi de fichier si le serveur n'a pas d'hébergement. */
    @GetMapping("/capacites")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Indique si l'envoi de fichier est disponible")
    public ResponseEntity<Map<String, Boolean>> capacites() {
        return ResponseEntity.ok(Map.of("hebergementDisponible", service.hebergementDisponible()));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Envoyer une image dans la galerie")
    public ResponseEntity<MediaResidenceResponse> televerser(
            @RequestPart("fichier") MultipartFile fichier,
            @RequestPart(value = "titre", required = false) String titre,
            @RequestPart(value = "categorie", required = false) String categorie) {
        return ResponseEntity.ok(service.televerser(fichier, titre, categorie));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Référencer une image déjà en ligne")
    public ResponseEntity<MediaResidenceResponse> create(@Valid @RequestBody MediaResidenceRequest request) {
        return ResponseEntity.ok(service.creerDepuisUrl(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Modifier un visuel")
    public ResponseEntity<MediaResidenceResponse> update(
            @PathVariable Long id, @Valid @RequestBody MediaResidenceRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/deplacer")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Monter ou descendre un visuel dans la galerie")
    public ResponseEntity<List<MediaResidenceResponse>> deplacer(
            @PathVariable Long id, @RequestParam int sens) {
        return ResponseEntity.ok(service.deplacer(id, sens));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Supprimer un visuel")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
