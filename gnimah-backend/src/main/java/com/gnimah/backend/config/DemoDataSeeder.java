package com.gnimah.backend.config;

import com.gnimah.backend.entity.*;
import com.gnimah.backend.entity.enums.*;
import com.gnimah.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Peuple des données de démonstration réalistes (clients, réservations, séjours,
 * paiements, tâches d'entretien, événements) au premier démarrage, pour que le
 * dashboard et les listes ne soient plus vides. S'exécute après {@link DataInitializer}
 * (les séjours/réservations ont besoin des comptes agent/directeur déjà créés).
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final ClientRepository clientRepository;
    private final ChambreRepository chambreRepository;
    private final ReservationRepository reservationRepository;
    private final SejourRepository sejourRepository;
    private final PaiementRepository paiementRepository;
    private final TacheEntretienRepository tacheEntretienRepository;
    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;

    private static final DateTimeFormatter RECU_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clientRepository.count() > 0) {
            log.info("Données de démonstration déjà présentes — skip.");
            return;
        }

        Utilisateur agent = utilisateurRepository.findByUsername("agent").orElse(null);
        Utilisateur directeur = utilisateurRepository.findByUsername("directeur").orElse(null);

        Client fatou = saveClient("Mme", "Diallo", "Fatou", "+225 05 44 55 66", "fatou.diallo@example.com", "CNI", "CI0099887");
        Client konan = saveClient("M.", "Yao", "Konan", "+225 07 11 22 33", null, "CNI", "CI0011223");
        Client aya = saveClient("Mme", "Kouassi", "Aya", "+225 01 22 33 44", "aya.kouassi@example.com", "CNI", "CI0022334");
        Client ibrahim = saveClient("M.", "Traoré", "Ibrahim", "+225 05 66 77 88", null, "Passeport", "CI0033445");
        Client adjoua = saveClient("Mme", "N'Guessan", "Adjoua", "+225 07 88 99 00", null, "CNI", "CI0044556");
        Client sekou = saveClient("M.", "Bamba", "Sekou", "+225 01 55 66 77", null, "CNI", "CI0055667");

        Map<String, Chambre> chambres = Map.ofEntries(
                Map.entry("101", chambreRepository.findByNumero("101").orElseThrow()),
                Map.entry("102", chambreRepository.findByNumero("102").orElseThrow()),
                Map.entry("103", chambreRepository.findByNumero("103").orElseThrow()),
                Map.entry("105", chambreRepository.findByNumero("105").orElseThrow()),
                Map.entry("201", chambreRepository.findByNumero("201").orElseThrow()),
                Map.entry("202", chambreRepository.findByNumero("202").orElseThrow()),
                Map.entry("203", chambreRepository.findByNumero("203").orElseThrow())
        );

        LocalDateTime now = LocalDateTime.now();

        // --- Réservations à venir (sur des chambres qui resteront libres, pas celles occupées par les séjours en cours ci-dessous) ---
        saveReservation("RES-DEMO-0001", fatou, chambres.get("203"), null, agent,
                now.plusDays(1).withHour(14).withMinute(0), now.plusDays(3).withHour(11).withMinute(0),
                StatutReservation.EN_ATTENTE, new BigDecimal("20000"));
        saveReservation("RES-DEMO-0002", aya, chambres.get("105"), null, directeur,
                now.plusDays(3).withHour(14).withMinute(0), now.plusDays(4).withHour(11).withMinute(0),
                StatutReservation.CONFIRMEE, new BigDecimal("15000"));
        saveReservation("RES-DEMO-0003", ibrahim, null, TypeChambre.STANDARD, agent,
                now.plusDays(5).withHour(14).withMinute(0), now.plusDays(7).withHour(11).withMinute(0),
                StatutReservation.EN_ATTENTE, BigDecimal.ZERO);

        // --- Séjours en cours (occupent 2 chambres) ---
        seedSejourEnCours("101", chambres, konan, agent, now.minusDays(1), new BigDecimal("30000"), new BigDecimal("15000"));
        seedSejourEnCours("201", chambres, adjoua, directeur, now.minusHours(20), new BigDecimal("30000"), new BigDecimal("30000"));

        // --- Séjours terminés (historique, 6 derniers jours) — Fatou en cumule 3 pour le badge "Fidèle" ---
        seedSejourTermine("102", chambres, sekou, agent, now.minusDays(6), now.minusDays(5), new BigDecimal("15000"), new BigDecimal("15000"));
        seedSejourTermine("103", chambres, fatou, agent, now.minusDays(5), now.minusDays(3), new BigDecimal("30000"), new BigDecimal("30000"));
        seedSejourTermine("105", chambres, fatou, directeur, now.minusDays(3), now.minusDays(2), new BigDecimal("20000"), new BigDecimal("20000"));
        seedSejourTermine("202", chambres, fatou, agent, now.minusDays(2), now.minusDays(1), new BigDecimal("30000"), new BigDecimal("20000"));

        // --- Tâches d'entretien ---
        saveTache(chambreRepository.findByNumero("204").orElseThrow(), "NETTOYAGE",
                "Nettoyage complet après séjour famille", StatutTache.A_FAIRE, 2, null);
        saveTache(chambreRepository.findByNumero("301").orElseThrow(), "MAINTENANCE",
                "Climatisation à vérifier — bruit anormal", StatutTache.EN_COURS, 3, agent);
        saveTache(chambreRepository.findByNumero("303").orElseThrow(), "INSPECTION",
                "Inspection avant remise en service", StatutTache.A_FAIRE, 2, directeur);

        // --- Événements ---
        saveEvenement("Séminaire entreprise ABC", "REUNION", "Salle de conférence",
                now.plusDays(4).withHour(9).withMinute(0), now.plusDays(4).withHour(17).withMinute(0),
                25, new BigDecimal("150000"), null, agent);
        saveEvenement("Mariage famille Kouassi", "MARIAGE", "Jardin",
                now.plusDays(10).withHour(15).withMinute(0), now.plusDays(10).withHour(21).withMinute(0),
                80, new BigDecimal("500000"), aya, directeur);

        log.info("✅ Données de démonstration créées : 6 clients, 3 réservations, 6 séjours, tâches d'entretien, événements.");
    }

    private Client saveClient(String civilite, String nom, String prenom, String telephone, String email, String typePiece, String numeroPiece) {
        Client client = Client.builder()
                .civilite(civilite)
                .nom(nom.toUpperCase())
                .prenom(prenom)
                .telephone(telephone)
                .email(email)
                .typePiece(typePiece)
                .numeroPiece(numeroPiece)
                .nationalite("Ivoirienne")
                .adresse("Abidjan, Abobo")
                .nbSejours(0)
                .build();
        return clientRepository.save(client);
    }

    private void saveReservation(String numero, Client client, Chambre chambre, TypeChambre typeChambre, Utilisateur agent,
                                  LocalDateTime arrivee, LocalDateTime depart, StatutReservation statut, BigDecimal acompte) {
        long nbNuits = java.time.Duration.between(arrivee, depart).toDays();
        BigDecimal montantPrevu = chambre != null ? chambre.getTarifNuitee().multiply(BigDecimal.valueOf(Math.max(nbNuits, 1))) : BigDecimal.ZERO;
        Reservation reservation = Reservation.builder()
                .numeroReservation(numero)
                .client(client)
                .chambre(chambre)
                .typeChambre(typeChambre)
                .agent(agent)
                .dateArrivee(arrivee)
                .dateDepart(depart)
                .nbNuits((int) nbNuits)
                .montantPrevu(montantPrevu)
                .acompte(acompte)
                .statut(statut)
                .build();
        reservationRepository.save(reservation);
    }

    private void seedSejourEnCours(String numeroChambre, Map<String, Chambre> chambres, Client client, Utilisateur agent,
                                    LocalDateTime dateEntree, BigDecimal montantTotal, BigDecimal montantPaye) {
        Chambre chambre = chambres.get(numeroChambre);
        Sejour sejour = Sejour.builder()
                .numeroRecu(numeroRecu(dateEntree, numeroChambre))
                .client(client)
                .chambre(chambre)
                .agent(agent)
                .typeLocation(TypeLocation.SEJOUR)
                .dateEntree(dateEntree)
                .nbJours(2)
                .montantTotal(montantTotal)
                .montantPaye(montantPaye)
                .resteAPayer(montantTotal.subtract(montantPaye))
                .statut("EN_COURS")
                .build();
        sejourRepository.save(sejour);
        savePaiement(sejour, montantPaye, agent);

        chambre.setEtat(EtatChambre.OCCUPEE);
        chambreRepository.save(chambre);

        client.setNbSejours(client.getNbSejours() + 1);
        clientRepository.save(client);
    }

    private void seedSejourTermine(String numeroChambre, Map<String, Chambre> chambres, Client client, Utilisateur agent,
                                    LocalDateTime dateEntree, LocalDateTime dateSortie, BigDecimal montantTotal, BigDecimal montantPaye) {
        Chambre chambre = chambres.get(numeroChambre);
        long nbJours = Math.max(java.time.Duration.between(dateEntree, dateSortie).toDays(), 1);
        Sejour sejour = Sejour.builder()
                .numeroRecu(numeroRecu(dateEntree, numeroChambre))
                .client(client)
                .chambre(chambre)
                .agent(agent)
                .typeLocation(TypeLocation.SEJOUR)
                .dateEntree(dateEntree)
                .dateSortie(dateSortie)
                .nbJours((int) nbJours)
                .montantTotal(montantTotal)
                .montantPaye(montantPaye)
                .resteAPayer(montantTotal.subtract(montantPaye))
                .statut("TERMINE")
                .build();
        sejourRepository.save(sejour);
        savePaiement(sejour, montantPaye, agent);

        client.setNbSejours(client.getNbSejours() + 1);
        clientRepository.save(client);
    }

    private void savePaiement(Sejour sejour, BigDecimal montant, Utilisateur agent) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) return;
        Paiement paiement = Paiement.builder()
                .sejour(sejour)
                .montant(montant)
                .mode(ModePaiement.ESPECES)
                .datePaiement(sejour.getDateEntree())
                .agent(agent)
                .build();
        paiementRepository.save(paiement);
    }

    private void saveTache(Chambre chambre, String type, String description, StatutTache statut, int priorite, Utilisateur agent) {
        TacheEntretien tache = TacheEntretien.builder()
                .chambre(chambre)
                .typeTache(type)
                .description(description)
                .statut(statut)
                .priorite(priorite)
                .agent(agent)
                .dateDebut(statut == StatutTache.EN_COURS ? LocalDateTime.now() : null)
                .build();
        tacheEntretienRepository.save(tache);
    }

    private void saveEvenement(String intitule, String type, String salle, LocalDateTime debut, LocalDateTime fin,
                                int nbPersonnes, BigDecimal montant, Client client, Utilisateur agent) {
        Evenement evenement = Evenement.builder()
                .intitule(intitule)
                .typeEvenement(type)
                .salle(salle)
                .dateDebut(debut)
                .dateFin(fin)
                .nbPersonnes(nbPersonnes)
                .montant(montant)
                .statut(StatutEvenement.PLANIFIE)
                .client(client)
                .agent(agent)
                .build();
        evenementRepository.save(evenement);
    }

    private String numeroRecu(LocalDateTime reference, String suffix) {
        return "REC-" + reference.format(RECU_FMT) + "-" + suffix;
    }
}
