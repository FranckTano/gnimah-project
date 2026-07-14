package com.gnimah.backend.controller;

import com.gnimah.backend.entity.Depense;
import com.gnimah.backend.repository.DepenseRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/depenses")
@RequiredArgsConstructor
@Tag(name = "Gestion des dépenses")
public class DepenseController {

    private final DepenseRepository depenseRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public ResponseEntity<List<Depense>> findAll() {
        return ResponseEntity.ok(depenseRepository.findAll());
    }

    @GetMapping("/periode")
    public ResponseEntity<List<Depense>> findByPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        return ResponseEntity.ok(depenseRepository.findByDateDepenseBetween(
                LocalDate.parse(debut), LocalDate.parse(fin)));
    }

    @PostMapping
    public ResponseEntity<Depense> create(@RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Depense depense = Depense.builder()
                .libelle(body.get("libelle").toString())
                .montant(new BigDecimal(body.get("montant").toString()))
                .categorie(body.getOrDefault("categorie", "DIVERS").toString())
                .dateDepense(body.containsKey("date") ? LocalDate.parse(body.get("date").toString()) : LocalDate.now())
                .notes(body.containsKey("notes") ? body.get("notes").toString() : null)
                .agent(utilisateurRepository.findByUsername(username).orElse(null))
                .build();
        return ResponseEntity.ok(depenseRepository.save(depense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        depenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
