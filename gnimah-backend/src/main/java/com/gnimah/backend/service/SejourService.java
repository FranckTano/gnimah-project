package com.gnimah.backend.service;

import com.gnimah.backend.dto.sejour.RemiseSejourRequest;
import com.gnimah.backend.dto.sejour.SejourRequest;
import com.gnimah.backend.dto.sejour.SejourResponse;
import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Paiement;
import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.Sejour;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeLocation;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
import com.gnimah.backend.entity.enums.TypeRemise;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.PaiementRepository;
import com.gnimah.backend.repository.ReservationRepository;
import com.gnimah.backend.repository.SejourRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SejourService {

    private final SejourRepository sejourRepository;
    private final ClientRepository clientRepository;
    private final ChambreService chambreService;
    private final UtilisateurRepository utilisateurRepository;
    private final PaiementRepository paiementRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final CaisseService caisseService;
    private final ReservationRepository reservationRepository;

    @Transactional
    public SejourResponse checkIn(SejourRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));
        Chambre chambre = chambreService.findById(request.getChambreId());

        if (chambre.getEtat() != EtatChambre.LIBRE) {
            throw new BusinessException("La chambre " + chambre.getNumero() + " n'est pas disponible (état: " + chambre.getEtat() + ")");
        }

        // Arrivée d'un client qui avait réservé : l'acompte qu'il a versé lui
        // reste acquis et vient en déduction de sa note.
        Reservation reservation = request.getReservationId() != null
                ? reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Réservation", request.getReservationId()))
                : null;
        BigDecimal acompteReservation = BigDecimal.ZERO;
        if (reservation != null) {
            if (reservation.getStatut() == StatutReservation.ANNULEE) {
                throw new BusinessException("La réservation " + reservation.getNumeroReservation() + " est annulée");
            }
            if (reservation.getStatut() == StatutReservation.ARRIVEE) {
                throw new BusinessException("La réservation " + reservation.getNumeroReservation()
                        + " a déjà donné lieu à un séjour");
            }
            if (!reservation.getClient().getId().equals(client.getId())) {
                throw new BusinessException("La réservation " + reservation.getNumeroReservation()
                        + " est au nom de " + reservation.getClient().getNomComplet());
            }
            acompteReservation = reservation.getAcompte() != null ? reservation.getAcompte() : BigDecimal.ZERO;
        }

        TypeLocation typeLocation = TypeLocation.valueOf(request.getTypeLocation());

        // Le tarif appliqué peut être négocié ; à défaut c'est celui de la chambre.
        BigDecimal tarifApplique = request.getTarifPersonnalise() != null
                && request.getTarifPersonnalise().compareTo(BigDecimal.ZERO) > 0
                ? request.getTarifPersonnalise()
                : tarifCatalogue(typeLocation, chambre);

        int nbUnites = nbUnites(typeLocation, request);
        BigDecimal montantBrut = tarifApplique.multiply(BigDecimal.valueOf(nbUnites));

        TypeRemise remiseType = Remises.parse(request.getRemiseType());
        BigDecimal remiseMontant = Remises.calculer(montantBrut, remiseType, request.getRemiseValeur());
        BigDecimal montantTotal = montantBrut.subtract(remiseMontant);

        BigDecimal versementArrivee = request.getMontantPaye() != null ? request.getMontantPaye() : BigDecimal.ZERO;
        if (versementArrivee.signum() < 0) {
            throw new BusinessException("Le montant encaissé ne peut pas être négatif");
        }
        BigDecimal montantPaye = acompteReservation.add(versementArrivee);
        if (montantPaye.compareTo(montantTotal) > 0) {
            throw new BusinessException(acompteReservation.signum() > 0
                    ? "L'acompte de la réservation (" + acompteReservation + " FCFA) et le versement d'arrivée ("
                        + versementArrivee + " FCFA) dépassent le montant du séjour (" + montantTotal + " FCFA)"
                    : "L'acompte (" + montantPaye + " FCFA) dépasse le montant du séjour (" + montantTotal + " FCFA)");
        }
        BigDecimal resteAPayer = montantTotal.subtract(montantPaye);

        String numeroRecu = genererNumeroRecu();
        Utilisateur agent = getAgentConnecte();

        // Le frontend envoie des dates au format "yyyy-MM-dd" (LocalDate), on convertit en LocalDateTime
        LocalDateTime dateEntreeDT = request.getDateEntree().atStartOfDay();
        LocalDateTime dateSortieDT = request.getDateSortie() != null ? request.getDateSortie().atStartOfDay() : null;
        if (typeLocation == TypeLocation.SEJOUR) {
            verifierReservationsConfirmees(chambre, dateEntreeDT,
                    dateSortieDT != null ? dateSortieDT : dateEntreeDT.plusDays(nbUnites), reservation);
        }

        Sejour sejour = Sejour.builder()
                .numeroRecu(numeroRecu)
                .client(client)
                .chambre(chambre)
                .agent(agent)
                .typeLocation(typeLocation)
                .dateEntree(dateEntreeDT)
                .dateSortie(dateSortieDT)
                .heureEntree(request.getHeureEntree())
                .heureSortie(request.getHeureSortie())
                .montantBrut(montantBrut)
                .tarifApplique(tarifApplique)
                .remiseType(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? remiseType : null)
                .remiseValeur(request.getRemiseValeur() != null ? request.getRemiseValeur() : BigDecimal.ZERO)
                .remiseMontant(remiseMontant)
                .remiseMotif(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? request.getRemiseMotif() : null)
                .remisePar(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? agent : null)
                .montantExtras(BigDecimal.ZERO)
                .montantTotal(montantTotal)
                .montantPaye(montantPaye)
                .resteAPayer(resteAPayer)
                .statut("EN_COURS")
                .notes(request.getNotes())
                .reservationId(reservation != null ? reservation.getId() : null)
                .build();

        if (typeLocation == TypeLocation.SEJOUR) {
            sejour.setNbJours(nbUnites);
        }

        chambre.setEtat(EtatChambre.OCCUPEE);
        client.setNbSejours(client.getNbSejours() + 1);
        clientRepository.save(client);

        Sejour saved = sejourRepository.save(sejour);

        // L'acompte de la réservation est en caisse depuis son encaissement : il
        // est reporté sur le séjour par une ligne de paiement, sans nouvelle
        // écriture de caisse — l'argent ne serait sinon compté deux fois.
        if (acompteReservation.signum() > 0) {
            paiementRepository.save(Paiement.builder()
                    .sejour(saved)
                    .montant(acompteReservation)
                    .mode(reservation.getModeAcompte() != null ? reservation.getModeAcompte() : ModePaiement.ESPECES)
                    .referenceTransaction(reservation.getReferenceAcompte())
                    .agent(agent)
                    .datePaiement(reservation.getDateAcompte() != null ? reservation.getDateAcompte() : LocalDateTime.now())
                    .notes("Acompte versé à la réservation " + reservation.getNumeroReservation())
                    .build());
        }

        // Créer un enregistrement Paiement si un acompte a été reçu à l'arrivée
        if (versementArrivee.compareTo(BigDecimal.ZERO) > 0) {
            ModePaiement mode = ModePaiement.ESPECES;
            if (request.getModePaiement() != null && !request.getModePaiement().isBlank()) {
                try { mode = ModePaiement.valueOf(request.getModePaiement()); } catch (IllegalArgumentException ignored) {}
            }
            Paiement paiement = Paiement.builder()
                    .sejour(saved)
                    .montant(versementArrivee)
                    .mode(mode)
                    .agent(agent)
                    .datePaiement(LocalDateTime.now())
                    .build();
            paiementRepository.save(paiement);

            Caisse caisse = caisseService.resoudreCaisse(request.getCaisseId());
            caisseService.enregistrer(caisse, SensMouvement.ENTREE, TypeMouvementCaisse.ENCAISSEMENT_SEJOUR,
                    versementArrivee, mode,
                    "Acompte à l'arrivée — chambre " + chambre.getNumero() + " (" + client.getNomComplet() + ")",
                    numeroRecu, "SEJOUR", saved.getId(), agent);
        }

        if (reservation != null) {
            reservation.setStatut(StatutReservation.ARRIVEE);
            reservationRepository.save(reservation);
        }

        auditService.log(agent, "CHECK_IN", "Sejour", saved.getId(), "Check-in chambre " + chambre.getNumero() + " pour " + client.getNomComplet()
                + (reservation != null ? " (réservation " + reservation.getNumeroReservation() + ")" : ""));
        if (remiseMontant.compareTo(BigDecimal.ZERO) > 0) {
            auditService.log(agent, "REMISE_SEJOUR", "Sejour", saved.getId(),
                    "Remise de " + remiseMontant + " FCFA sur " + montantBrut + " FCFA"
                            + (request.getRemiseMotif() != null ? " — " + request.getRemiseMotif() : ""));
        }

        return toResponse(saved);
    }

    /**
     * Révise le prix d'un séjour en cours : tarif négocié et/ou remise. Le
     * montant déjà encaissé est préservé et le reste à payer recalculé, ce qui
     * permet de corriger une note après un premier versement.
     */
    @Transactional
    public SejourResponse appliquerRemise(Long id, RemiseSejourRequest request) {
        Sejour sejour = findById(id);
        if (!"EN_COURS".equals(sejour.getStatut())) {
            throw new BusinessException("Seul un séjour en cours peut voir son prix révisé");
        }

        BigDecimal tarifApplique = request.getTarifPersonnalise() != null
                && request.getTarifPersonnalise().compareTo(BigDecimal.ZERO) > 0
                ? request.getTarifPersonnalise()
                : (sejour.getTarifApplique() != null
                    ? sejour.getTarifApplique()
                    : tarifCatalogue(sejour.getTypeLocation(), sejour.getChambre()));

        int nbUnites = sejour.getTypeLocation() == TypeLocation.SEJOUR
                ? Math.max(sejour.getNbJours() != null ? sejour.getNbJours() : 1, 1) : 1;
        LocalDateTime nouvelleSortie = null;
        if (sejour.getTypeLocation() == TypeLocation.SEJOUR && request.getDateSortie() != null) {
            nouvelleSortie = request.getDateSortie().atStartOfDay();
            nbUnites = nuiteesJusquA(sejour, nouvelleSortie);
        }
        BigDecimal montantBrut = tarifApplique.multiply(BigDecimal.valueOf(nbUnites));

        TypeRemise remiseType = Remises.parse(request.getRemiseType());
        BigDecimal remiseMontant = Remises.calculer(montantBrut, remiseType, request.getRemiseValeur());

        BigDecimal extras = sejour.getMontantExtras() != null ? sejour.getMontantExtras() : BigDecimal.ZERO;
        BigDecimal nouveauTotal = montantBrut.subtract(remiseMontant).add(extras);

        if (nouveauTotal.compareTo(sejour.getMontantPaye()) < 0) {
            throw new BusinessException("Le nouveau montant (" + nouveauTotal + " FCFA) est inférieur à ce que le "
                    + "client a déjà versé (" + sejour.getMontantPaye() + " FCFA) — un remboursement est nécessaire.");
        }

        Utilisateur agent = getAgentConnecte();
        if (nouvelleSortie != null) {
            if (!nouvelleSortie.equals(sejour.getDateSortie())) {
                auditService.log(agent, "MODIFICATION_DATES_SEJOUR", "Sejour", sejour.getId(),
                        "Départ prévu le " + request.getDateSortie() + " (" + nbUnites + " nuit"
                                + (nbUnites > 1 ? "s" : "") + ")");
            }
            sejour.setDateSortie(nouvelleSortie);
            sejour.setNbJours(nbUnites);
        }
        sejour.setTarifApplique(tarifApplique);
        sejour.setMontantBrut(montantBrut);
        sejour.setRemiseType(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? remiseType : null);
        sejour.setRemiseValeur(request.getRemiseValeur() != null ? request.getRemiseValeur() : BigDecimal.ZERO);
        sejour.setRemiseMontant(remiseMontant);
        sejour.setRemiseMotif(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? request.getRemiseMotif() : null);
        sejour.setRemisePar(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? agent : null);
        sejour.setMontantTotal(nouveauTotal);
        sejour.setResteAPayer(nouveauTotal.subtract(sejour.getMontantPaye()));

        auditService.log(agent, "REVISION_PRIX_SEJOUR", "Sejour", sejour.getId(),
                "Nouveau montant " + nouveauTotal + " FCFA (brut " + montantBrut
                        + ", remise " + remiseMontant + ")"
                        + (request.getRemiseMotif() != null ? " — " + request.getRemiseMotif() : ""));

        return toResponse(sejourRepository.save(sejour));
    }

    @Transactional
    public SejourResponse checkOut(Long id) {
        Sejour sejour = findById(id);
        if (!"EN_COURS".equals(sejour.getStatut())) {
            throw new BusinessException("Ce séjour n'est pas en cours");
        }

        sejour.setStatut("TERMINE");
        sejour.setDateSortie(LocalDateTime.now());

        Chambre chambre = sejour.getChambre();
        chambre.setEtat(EtatChambre.A_NETTOYER);

        Utilisateur agent = getAgentConnecte();
        auditService.log(agent, "CHECK_OUT", "Sejour", sejour.getId(), "Check-out chambre " + chambre.getNumero());
        notificationService.creer("FIN_SEJOUR", "Séjour terminé",
                "Chambre " + chambre.getNumero() + " libérée par " + sejour.getClient().getNomComplet() + " — à nettoyer", "/entretien");

        // Le solde impayé n'empêche pas la sortie — l'arrangement se règle entre
        // la réception et le client —, mais il ne doit pas passer inaperçu.
        if (sejour.getResteAPayer().compareTo(BigDecimal.ZERO) > 0) {
            notificationService.creer("IMPAYE", "Départ avec solde impayé",
                    sejour.getClient().getNomComplet() + " — reste " + sejour.getResteAPayer()
                            + " FCFA sur le reçu " + sejour.getNumeroRecu(), "/paiements");
        }

        return toResponse(sejourRepository.save(sejour));
    }

    @Transactional(readOnly = true)
    public SejourResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<SejourResponse> findAll(Pageable pageable) {
        return sejourRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SejourResponse> findByClient(Long clientId, Pageable pageable) {
        return sejourRepository.findByClientId(clientId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<SejourResponse> findSejoursEnCours() {
        return sejourRepository.findSejoursEnCours().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SejourResponse> findDepartsJour() {
        LocalDateTime debut = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fin = debut.plusDays(1);
        return sejourRepository.findDepartsJour(debut, fin).stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Tarif catalogue de la chambre pour le mode de location retenu. */
    private BigDecimal tarifCatalogue(TypeLocation type, Chambre chambre) {
        return type == TypeLocation.SEJOUR ? chambre.getTarifNuitee() : chambre.getTarifPassage();
    }

    /**
     * Nuitées entre l'arrivée et une nouvelle date de départ. Une prolongation
     * ne doit pas empiéter sur la réservation d'un autre client pour la même
     * chambre : elle est refusée, l'agent proposera un changement de chambre.
     */
    private int nuiteesJusquA(Sejour sejour, LocalDateTime nouvelleSortie) {
        DateTimeFormatter jourFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDateTime arrivee = sejour.getDateEntree().toLocalDate().atStartOfDay();
        if (!nouvelleSortie.isAfter(arrivee)) {
            throw new BusinessException("La date de départ doit être postérieure à la date d'arrivée ("
                    + arrivee.format(jourFmt) + ")");
        }
        LocalDateTime sortiePrevue = sejour.getDateSortie() != null
                ? sejour.getDateSortie()
                : arrivee.plusDays(Math.max(sejour.getNbJours() != null ? sejour.getNbJours() : 1, 1));
        if (nouvelleSortie.isAfter(sortiePrevue)) {
            verifierReservationsConfirmees(sejour.getChambre(), sortiePrevue, nouvelleSortie, null);
        }
        return (int) ChronoUnit.DAYS.between(arrivee.toLocalDate(), nouvelleSortie.toLocalDate());
    }

    /**
     * Refuse d'occuper une chambre sur des nuits déjà promises à un autre client
     * par une réservation confirmée (acompte versé) : il trouverait sa chambre
     * prise à son arrivée. Une réservation encore en attente d'acompte ne tient
     * pas la chambre — c'est la règle de la maison — et ne bloque donc rien.
     */
    private void verifierReservationsConfirmees(Chambre chambre, LocalDateTime debut, LocalDateTime fin,
                                                Reservation honoree) {
        reservationRepository.findConflits(chambre.getId(), debut, fin).stream()
                .filter(r -> r.getStatut() == StatutReservation.CONFIRMEE)
                .filter(r -> honoree == null || !r.getId().equals(honoree.getId()))
                .findFirst()
                .ifPresent(r -> {
                    throw new BusinessException("La chambre " + chambre.getNumero() + " est réservée par "
                            + r.getClient().getNomComplet() + " à partir du "
                            + r.getDateArrivee().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + " : choisissez une autre chambre ou des dates plus courtes.");
                });
    }

    /** Nombre de nuitées facturées ; un passage est un forfait à l'unité. */
    private int nbUnites(TypeLocation type, SejourRequest request) {
        if (type != TypeLocation.SEJOUR || request.getDateSortie() == null) {
            return 1;
        }
        long nbJours = ChronoUnit.DAYS.between(request.getDateEntree(), request.getDateSortie());
        return (int) Math.max(nbJours, 1);
    }

    private String genererNumeroRecu() {
        String prefix = "REC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Integer maxSeq = sejourRepository.findMaxSequence(prefix);
        int seq = (maxSeq != null ? maxSeq : 0) + 1;
        return prefix + String.format("%04d", seq);
    }

    private Utilisateur getAgentConnecte() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByUsername(username).orElse(null);
    }

    public Sejour findById(Long id) {
        return sejourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séjour", id));
    }

    public SejourResponse toResponse(Sejour s) {
        return SejourResponse.builder()
                .id(s.getId())
                .numeroRecu(s.getNumeroRecu())
                .clientId(s.getClient().getId())
                .clientNom(s.getClient().getNomComplet())
                .clientTelephone(s.getClient().getTelephone())
                .chambreId(s.getChambre().getId())
                .chambreNumero(s.getChambre().getNumero())
                .chambreType(s.getChambre().getType().name())
                .agentNom(s.getAgent() != null ? s.getAgent().getNomComplet() : null)
                .typeLocation(s.getTypeLocation().name())
                .dateEntree(s.getDateEntree())
                .dateSortie(s.getDateSortie())
                .heureEntree(s.getHeureEntree())
                .heureSortie(s.getHeureSortie())
                .nbJours(s.getNbJours())
                .nbHeures(s.getNbHeures())
                .montantBrut(s.getMontantBrut())
                .tarifApplique(s.getTarifApplique())
                .remiseType(s.getRemiseType() != null ? s.getRemiseType().name() : null)
                .remiseValeur(s.getRemiseValeur())
                .remiseMontant(s.getRemiseMontant())
                .remiseMotif(s.getRemiseMotif())
                .remiseParNom(s.getRemisePar() != null ? s.getRemisePar().getNomComplet() : null)
                .montantExtras(s.getMontantExtras())
                .montantTotal(s.getMontantTotal())
                .montantPaye(s.getMontantPaye())
                .resteAPayer(s.getResteAPayer())
                .statut(s.getStatut())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
