package com.gnimah.backend.controller;

import com.gnimah.backend.entity.Evenement;
import com.gnimah.backend.entity.enums.StatutEvenement;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.EvenementRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evenements")
@RequiredArgsConstructor
@Tag(name = "Gestion des événements")
public class EvenementController {

    private final EvenementRepository evenementRepository;
    private final ClientRepository clientRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public ResponseEntity<List<Evenement>> findAll() {
        return ResponseEntity.ok(evenementRepository.findAll());
    }

    @GetMapping("/periode")
    public ResponseEntity<List<Evenement>> findByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(evenementRepository.findByPeriode(debut, fin));
    }

    @PostMapping
    public ResponseEntity<Evenement> create(@RequestBody Map<String, Object> body) {
        Evenement evenement = Evenement.builder()
                .intitule(body.get("intitule").toString())
                .typeEvenement(body.getOrDefault("typeEvenement", "REUNION").toString())
                .salle(body.containsKey("salle") ? body.get("salle").toString() : null)
                .dateDebut(LocalDateTime.parse(body.get("dateDebut").toString()))
                .dateFin(body.containsKey("dateFin") ? LocalDateTime.parse(body.get("dateFin").toString()) : null)
                .nbPersonnes(body.containsKey("nbPersonnes") ? Integer.parseInt(body.get("nbPersonnes").toString()) : 1)
                .montant(body.containsKey("montant") ? new BigDecimal(body.get("montant").toString()) : BigDecimal.ZERO)
                .statut(StatutEvenement.PLANIFIE)
                .description(body.containsKey("description") ? body.get("description").toString() : null)
                .build();

        if (body.containsKey("clientId")) {
            Long clientId = Long.parseLong(body.get("clientId").toString());
            clientRepository.findById(clientId).ifPresent(evenement::setClient);
        }

        return ResponseEntity.ok(evenementRepository.save(evenement));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<Evenement> updateStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Evenement evenement = evenementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement", id));
        evenement.setStatut(StatutEvenement.valueOf(body.get("statut")));
        return ResponseEntity.ok(evenementRepository.save(evenement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        evenementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
