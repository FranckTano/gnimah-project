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

    /**
     * Dit au frontend par quelle voie téléverser.
     *
     * Si le serveur détient les identifiants Cloudinary, il signe l'envoi
     * lui-même — c'est la voie préférée, le secret ne quitte pas le serveur.
     * Sinon le navigateur téléverse directement via un préréglage non signé,
     * dont le nom n'est pas un secret. Dans les deux cas l'administrateur choisit
     * simplement un fichier ; la différence lui est invisible.
     */
    @GetMapping("/capacites")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Voie de téléversement disponible")
    public ResponseEntity<Map<String, Object>> capacites() {
        return ResponseEntity.ok(Map.of(
                "hebergementDisponible", service.hebergementDisponible(),
                "cloudName", service.cloudName(),
                "uploadPreset", service.uploadPreset(),
                "dossier", service.dossier()));
    }

    /**
     * Enregistre une image que le navigateur vient de déposer sur Cloudinary.
     * Le serveur vérifie qu'elle appartient bien au compte et au dossier de la
     * galerie avant de la retenir.
     */
    @PostMapping("/televersement")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
    @Operation(summary = "Enregistrer une image téléversée par le navigateur")
    public ResponseEntity<MediaResidenceResponse> enregistrerTeleversement(
            @Valid @RequestBody MediaResidenceRequest request) {
        return ResponseEntity.ok(service.enregistrerTeleversement(request));
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
