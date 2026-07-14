package com.gnimah.backend.controller;

import com.gnimah.backend.entity.TacheEntretien;
import com.gnimah.backend.entity.enums.StatutTache;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ChambreRepository;
import com.gnimah.backend.repository.TacheEntretienRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/entretien")
@RequiredArgsConstructor
@Tag(name = "Entretien / Housekeeping")
public class TacheEntretienController {

    private final TacheEntretienRepository tacheRepository;
    private final ChambreRepository chambreRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> findAll(@RequestParam(required = false) String statut) {
        List<TacheEntretien> list = (statut != null && !statut.isBlank())
                ? tacheRepository.findByStatut(StatutTache.valueOf(statut))
                : tacheRepository.findAll();
        return ResponseEntity.ok(list.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<Map<String, Object>>> findEnAttente() {
        return ResponseEntity.ok(tacheRepository.findByStatut(StatutTache.A_FAIRE)
                .stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/chambre/{chambreId}")
    public ResponseEntity<List<Map<String, Object>>> findByChambre(@PathVariable Long chambreId) {
        return ResponseEntity.ok(tacheRepository.findByChambreId(chambreId)
                .stream().map(this::toDto).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long chambreId = Long.parseLong(body.get("chambreId").toString());
        String type = body.containsKey("type") ? body.get("type").toString()
                : body.getOrDefault("typeTache", "NETTOYAGE").toString();
        TacheEntretien tache = TacheEntretien.builder()
                .chambre(chambreRepository.findById(chambreId)
                        .orElseThrow(() -> new ResourceNotFoundException("Chambre", chambreId)))
                .typeTache(type)
                .description(body.containsKey("description") ? body.get("description").toString() : null)
                .statut(StatutTache.A_FAIRE)
                .priorite(2)
                .build();
        return ResponseEntity.ok(toDto(tacheRepository.save(tache)));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<Map<String, Object>> updateStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TacheEntretien tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche", id));
        StatutTache statut = StatutTache.valueOf(body.get("statut"));
        tache.setStatut(statut);
        if (statut == StatutTache.EN_COURS && tache.getDateDebut() == null) {
            tache.setDateDebut(LocalDateTime.now());
        } else if (statut == StatutTache.TERMINE) {
            tache.setDateFin(LocalDateTime.now());
        }
        return ResponseEntity.ok(toDto(tacheRepository.save(tache)));
    }

    private Map<String, Object> toDto(TacheEntretien t) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", t.getId());
        dto.put("chambreId", t.getChambre().getId());
        dto.put("chambreNumero", t.getChambre().getNumero());
        dto.put("type", t.getTypeTache());
        dto.put("description", t.getDescription());
        dto.put("statut", t.getStatut().name());
        dto.put("priorite", t.getPriorite() >= 3 ? "URGENTE" : "NORMALE");
        dto.put("assigneA", t.getAgent() != null ? t.getAgent().getNomComplet() : null);
        dto.put("dateCreation", t.getCreatedAt());
        dto.put("dateFin", t.getDateFin());
        return dto;
    }
}
