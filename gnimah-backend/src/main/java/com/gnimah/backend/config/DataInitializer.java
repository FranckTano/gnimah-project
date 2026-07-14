package com.gnimah.backend.config;

import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.repository.ChambreRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final ChambreRepository chambreRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initUtilisateurs();
        initChambres();
    }

    private void initUtilisateurs() {
        if (utilisateurRepository.existsByUsername("admin")) {
            log.info("Utilisateurs déjà initialisés.");
            return;
        }

        List<Utilisateur> utilisateurs = List.of(
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

        utilisateurRepository.saveAll(utilisateurs);
        log.info("✅ Utilisateurs par défaut créés (admin, directeur, agent)");
    }

    private void initChambres() {
        if (chambreRepository.count() > 0) {
            log.info("Chambres déjà initialisées.");
            return;
        }

        List<Chambre> chambres = List.of(
            chambre("101", TypeChambre.STANDARD,  2, 5000,  15000, 1, "Climatisation,TV,WiFi,Salle de bain"),
            chambre("102", TypeChambre.STANDARD,  2, 5000,  15000, 1, "Climatisation,TV,WiFi,Salle de bain"),
            chambre("103", TypeChambre.STANDARD,  2, 5000,  15000, 1, "Climatisation,TV,WiFi,Salle de bain"),
            chambre("104", TypeChambre.SUPERIEURE, 2, 7000, 20000, 1, "Climatisation,TV,WiFi,Salle de bain,Minibar"),
            chambre("105", TypeChambre.SUPERIEURE, 2, 7000, 20000, 1, "Climatisation,TV,WiFi,Salle de bain,Minibar"),
            chambre("201", TypeChambre.DELUXE,    3, 10000, 30000, 2, "Climatisation,TV,WiFi,Salle de bain,Minibar,Balcon"),
            chambre("202", TypeChambre.DELUXE,    3, 10000, 30000, 2, "Climatisation,TV,WiFi,Salle de bain,Minibar"),
            chambre("203", TypeChambre.SUITE,     4, 15000, 50000, 2, "Climatisation,TV,WiFi,Salle de bain,Minibar,Salon,Balcon"),
            chambre("204", TypeChambre.FAMILIALE, 5, 12000, 40000, 2, "Climatisation,TV,WiFi,2 Salles de bain"),
            chambre("301", TypeChambre.STANDARD,  2, 5000,  15000, 3, "Climatisation,TV,WiFi,Salle de bain"),
            chambre("302", TypeChambre.STANDARD,  2, 5000,  15000, 3, "Climatisation,TV,WiFi,Salle de bain"),
            chambre("303", TypeChambre.SUPERIEURE, 2, 7000, 20000, 3, "Climatisation,TV,WiFi,Salle de bain,Minibar")
        );

        chambreRepository.saveAll(chambres);
        log.info("✅ {} chambres créées", chambres.size());
    }

    private Chambre chambre(String numero, TypeChambre type, int capacite,
                             long tarifPassage, long tarifNuitee, int etage, String equipements) {
        return Chambre.builder()
                .numero(numero)
                .type(type)
                .capacite(capacite)
                .tarifPassage(BigDecimal.valueOf(tarifPassage))
                .tarifNuitee(BigDecimal.valueOf(tarifNuitee))
                .etat(EtatChambre.LIBRE)
                .etage(etage)
                .equipements(equipements)
                .build();
    }
}
