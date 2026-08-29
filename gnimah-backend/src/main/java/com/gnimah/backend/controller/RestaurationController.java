package com.gnimah.backend.controller;

import com.gnimah.backend.dto.restauration.*;
import com.gnimah.backend.service.RestaurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restauration")
@RequiredArgsConstructor
@Tag(name = "Restauration")
public class RestaurationController {

    private final RestaurationService restaurationService;

    // ---- Carte ----

    @GetMapping("/carte")
    @Operation(summary = "Consulter la carte")
    public ResponseEntity<List<ArticleRestaurationResponse>> carte(
            @RequestParam(defaultValue = "false") boolean tout) {
        return ResponseEntity.ok(restaurationService.carte(!tout));
    }

    @PostMapping("/carte")
    @Operation(summary = "Ajouter un article à la carte")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<ArticleRestaurationResponse> creerArticle(
            @Valid @RequestBody ArticleRestaurationRequest request) {
        return ResponseEntity.ok(restaurationService.creerArticle(request));
    }

    @PutMapping("/carte/{id}")
    @Operation(summary = "Modifier un article de la carte")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<ArticleRestaurationResponse> modifierArticle(
            @PathVariable Long id, @Valid @RequestBody ArticleRestaurationRequest request) {
        return ResponseEntity.ok(restaurationService.modifierArticle(id, request));
    }

    @PatchMapping("/carte/{id}/disponibilite")
    @Operation(summary = "Basculer la disponibilité d'un article")
    public ResponseEntity<ArticleRestaurationResponse> basculerDisponibilite(@PathVariable Long id) {
        return ResponseEntity.ok(restaurationService.basculerDisponibilite(id));
    }

    @DeleteMapping("/carte/{id}")
    @Operation(summary = "Retirer un article de la carte")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<Void> supprimerArticle(@PathVariable Long id) {
        restaurationService.supprimerArticle(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Commandes ----

    @GetMapping("/commandes")
    @Operation(summary = "Lister les additions")
    public ResponseEntity<Page<CommandeResponse>> commandes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(restaurationService.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/commandes/en-cours")
    @Operation(summary = "Additions ouvertes ou servies non réglées")
    public ResponseEntity<List<CommandeResponse>> enCours() {
        return ResponseEntity.ok(restaurationService.findEnCours());
    }

    @GetMapping("/commandes/{id}")
    @Operation(summary = "Détail d'une addition")
    public ResponseEntity<CommandeResponse> getCommande(@PathVariable Long id) {
        return ResponseEntity.ok(restaurationService.getById(id));
    }

    @GetMapping("/commandes/sejour/{sejourId}")
    @Operation(summary = "Additions rattachées à un séjour")
    public ResponseEntity<List<CommandeResponse>> parSejour(@PathVariable Long sejourId) {
        return ResponseEntity.ok(restaurationService.findBySejour(sejourId));
    }

    @PostMapping("/commandes")
    @Operation(summary = "Ouvrir une addition")
    public ResponseEntity<CommandeResponse> creer(@Valid @RequestBody CommandeRequest request) {
        return ResponseEntity.ok(restaurationService.creer(request));
    }

    @PutMapping("/commandes/{id}")
    @Operation(summary = "Modifier une addition non réglée")
    public ResponseEntity<CommandeResponse> modifier(@PathVariable Long id,
                                                     @Valid @RequestBody CommandeRequest request) {
        return ResponseEntity.ok(restaurationService.modifier(id, request));
    }

    @PatchMapping("/commandes/{id}/statut")
    @Operation(summary = "Changer le statut d'une addition")
    public ResponseEntity<CommandeResponse> changerStatut(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(restaurationService.changerStatut(id, body.get("statut")));
    }

    @PostMapping("/commandes/{id}/encaissement")
    @Operation(summary = "Encaisser l'addition ou la porter au compte du séjour")
    public ResponseEntity<CommandeResponse> encaisser(@PathVariable Long id,
                                                      @Valid @RequestBody PaiementCommandeRequest request) {
        return ResponseEntity.ok(restaurationService.encaisser(id, request));
    }

    @PostMapping("/commandes/{id}/annulation")
    @Operation(summary = "Annuler une addition non réglée")
    public ResponseEntity<CommandeResponse> annuler(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(restaurationService.annuler(id, body != null ? body.get("motif") : null));
    }
}
