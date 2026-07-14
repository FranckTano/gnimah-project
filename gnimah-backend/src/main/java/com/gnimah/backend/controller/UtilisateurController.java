package com.gnimah.backend.controller;

import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.UtilisateurRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Gestion des utilisateurs")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<Utilisateur>> findAll() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Utilisateur> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id)));
    }

    @PostMapping
    public ResponseEntity<Utilisateur> create(@RequestBody Map<String, String> body) {
        if (utilisateurRepository.existsByUsername(body.get("username"))) {
            throw new BusinessException("Ce nom d'utilisateur existe déjà");
        }
        Utilisateur utilisateur = Utilisateur.builder()
                .nom(body.get("nom"))
                .prenom(body.get("prenom"))
                .username(body.get("username"))
                .email(body.get("email"))
                .password(passwordEncoder.encode(body.get("password")))
                .role(Role.valueOf(body.getOrDefault("role", "AGENT")))
                .actif(true)
                .telephone(body.get("telephone"))
                .build();
        return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
    }

    @PatchMapping("/{id}/toggle-actif")
    public ResponseEntity<Utilisateur> toggleActif(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        utilisateur.setActif(!utilisateur.isActif());
        return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        utilisateur.setPassword(passwordEncoder.encode(body.get("password")));
        utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok().build();
    }
}
