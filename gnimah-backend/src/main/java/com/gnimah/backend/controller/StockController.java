package com.gnimah.backend.controller;

import com.gnimah.backend.dto.stock.ArticleStockRequest;
import com.gnimah.backend.dto.stock.ArticleStockResponse;
import com.gnimah.backend.dto.stock.MouvementStockRequest;
import com.gnimah.backend.dto.stock.MouvementStockResponse;
import com.gnimah.backend.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Tag(name = "Stock et provisions")
public class StockController {

    private final StockService stockService;

    @GetMapping("/articles")
    @Operation(summary = "Lister les articles en stock")
    public ResponseEntity<List<ArticleStockResponse>> articles(
            @RequestParam(required = false) String categorie) {
        return ResponseEntity.ok(categorie != null && !categorie.isBlank()
                ? stockService.findByCategorie(categorie)
                : stockService.findAll());
    }

    @GetMapping("/alertes")
    @Operation(summary = "Articles sous le seuil de réapprovisionnement")
    public ResponseEntity<List<ArticleStockResponse>> alertes() {
        return ResponseEntity.ok(stockService.findEnAlerte());
    }

    @GetMapping("/valeur")
    @Operation(summary = "Valeur totale du stock")
    public ResponseEntity<Map<String, BigDecimal>> valeur() {
        return ResponseEntity.ok(Map.of("valeur", stockService.valeurStock()));
    }

    @PostMapping("/articles")
    @Operation(summary = "Créer un article de stock")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<ArticleStockResponse> create(@Valid @RequestBody ArticleStockRequest request) {
        return ResponseEntity.ok(stockService.create(request));
    }

    @PutMapping("/articles/{id}")
    @Operation(summary = "Modifier un article de stock")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<ArticleStockResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody ArticleStockRequest request) {
        return ResponseEntity.ok(stockService.update(id, request));
    }

    @DeleteMapping("/articles/{id}")
    @Operation(summary = "Désactiver un article de stock")
    @PreAuthorize("hasAnyRole('RESPONSABLE','DIRECTEUR','ADMIN')")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        stockService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mouvements")
    @Operation(summary = "Enregistrer une entrée ou une sortie de stock")
    public ResponseEntity<MouvementStockResponse> enregistrer(@Valid @RequestBody MouvementStockRequest request) {
        return ResponseEntity.ok(stockService.enregistrer(request));
    }

    @GetMapping("/articles/{id}/mouvements")
    @Operation(summary = "Historique des mouvements d'un article")
    public ResponseEntity<List<MouvementStockResponse>> historique(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.historiqueArticle(id));
    }

    @GetMapping("/mouvements")
    @Operation(summary = "Journal des mouvements de stock")
    public ResponseEntity<Page<MouvementStockResponse>> journal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(stockService.journal(PageRequest.of(page, size)));
    }
}
