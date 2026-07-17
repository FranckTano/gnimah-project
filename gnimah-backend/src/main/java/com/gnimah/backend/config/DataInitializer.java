package com.gnimah.backend.config;

import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Vérifié compte par compte (pas juste "admin existe ?") : sur une base déjà initialisée par une
        // version antérieure du projet, un nouveau compte de démo ajouté plus tard (ex. "responsable")
        // serait sinon silencieusement ignoré puisque "admin" existait déjà.
        creerSiAbsent("admin", () -> Utilisateur.builder()
                .nom("Administrateur").prenom("Système")
                .username("admin").email("admin@gnimah.com")
                .password(passwordEncoder.encode("Admin@2026"))
                .role(Role.ADMIN).actif(true).build());

        creerSiAbsent("directeur", () -> Utilisateur.builder()
                .nom("GNIMAH").prenom("Directeur")
                .username("directeur").email("directeur@gnimah.com")
                .password(passwordEncoder.encode("Directeur@2026"))
                .role(Role.DIRECTEUR).actif(true).build());

        creerSiAbsent("responsable", () -> Utilisateur.builder()
                .nom("GNIMAH").prenom("Responsable")
                .username("responsable").email("responsable@gnimah.com")
                .password(passwordEncoder.encode("Responsable@2026"))
                .role(Role.RESPONSABLE).actif(true).build());

        creerSiAbsent("agent", () -> Utilisateur.builder()
                .nom("Réception").prenom("Agent")
                .username("agent").email("agent@gnimah.com")
                .password(passwordEncoder.encode("Agent@2026"))
                .role(Role.AGENT).actif(true).build());
    }

    private void creerSiAbsent(String username, Supplier<Utilisateur> builder) {
        if (utilisateurRepository.existsByUsername(username)) {
            return;
        }
        utilisateurRepository.save(builder.get());
        log.info("✅ Utilisateur créé : {}", username);
    }
}
