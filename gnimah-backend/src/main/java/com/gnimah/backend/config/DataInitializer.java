package com.gnimah.backend.config;

import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (utilisateurRepository.existsByUsername("admin")) {
            log.info("Utilisateurs déjà initialisés — skip.");
            return;
        }

        List<Utilisateur> defaults = List.of(
            Utilisateur.builder()
                .nom("Administrateur").prenom("Système")
                .username("admin").email("admin@gnimah.com")
                .password(passwordEncoder.encode("Admin@2026"))
                .role(Role.ADMIN).actif(true).build(),

            Utilisateur.builder()
                .nom("GNIMAH").prenom("Directeur")
                .username("directeur").email("directeur@gnimah.com")
                .password(passwordEncoder.encode("Directeur@2026"))
                .role(Role.DIRECTEUR).actif(true).build(),

            Utilisateur.builder()
                .nom("Réception").prenom("Agent")
                .username("agent").email("agent@gnimah.com")
                .password(passwordEncoder.encode("Agent@2026"))
                .role(Role.AGENT).actif(true).build()
        );

        utilisateurRepository.saveAll(defaults);
        log.info("✅ Utilisateurs créés : admin / directeur / agent");
    }
}
