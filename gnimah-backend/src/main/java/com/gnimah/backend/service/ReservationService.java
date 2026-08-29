package com.gnimah.backend.service;

import com.gnimah.backend.dto.reservation.AcompteRequest;
import com.gnimah.backend.dto.reservation.AnnulationRequest;
import com.gnimah.backend.dto.reservation.PolitiqueAnnulationResponse;
import com.gnimah.backend.dto.reservation.ReservationRequest;
import com.gnimah.backend.dto.reservation.ReservationResponse;
import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
import com.gnimah.backend.entity.enums.TypeRemise;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ReservationRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Réservations.
 *
 * Deux règles de maison structurent ce service :
 *
 * 1. Une réservation n'est confirmée que par un acompte. Tant qu'il n'est pas
 *    encaissé, elle reste EN_ATTENTE et la chambre n'est pas vraiment tenue —
 *    c'est ce qui protège la résidence des réservations de principe jamais
 *    honorées.
 *
 * 2. L'annulation doit être annoncée au moins 48 h avant l'arrivée. Passé ce
 *    délai, la chambre n'est plus relouable et l'acompte reste acquis à la
 *    résidence à titre de dédommagement.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    /** Préavis d'annulation sans frais, en heures. */
    public static final long PREAVIS_ANNULATION_HEURES = 48;

    /** Part du séjour exigée en acompte lorsque rien n'est précisé. */
    private static final BigDecimal TAUX_ACOMPTE_DEFAUT = BigDecimal.valueOf(0.30);

    private final ReservationRepository reservationRepository;
    private final ClientService clientService;
    private final ChambreService chambreService;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final CaisseService caisseService;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Création
    // ------------------------------------------------------------------

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Client client = clientService.findById(request.getClientId());
        Chambre chambre = null;

        // Le frontend envoie des dates "yyyy-MM-dd" (LocalDate) — on convertit en LocalDateTime pour l'entité
        LocalDateTime dateArriveDT = request.getDateArrivee().atStartOfDay();
        LocalDateTime dateDepartDT = request.getDateDepart().atStartOfDay();

        if (!dateDepartDT.isAfter(dateArriveDT)) {
            throw new BusinessException("La date de départ doit être postérieure à la date d'arrivée");
        }

        if (request.getChambreId() != null) {
            chambre = chambreService.findById(request.getChambreId());
            List<Reservation> conflits = reservationRepository.findConflits(
                    request.getChambreId(), dateArriveDT, dateDepartDT);
            if (!conflits.isEmpty()) {
                throw new BusinessException("La chambre est déjà réservée pour cette période");
            }
        }

        long nbNuits = Math.max(ChronoUnit.DAYS.between(request.getDateArrivee(), request.getDateDepart()), 1);

        BigDecimal tarif = request.getTarifPersonnalise() != null
                && request.getTarifPersonnalise().compareTo(BigDecimal.ZERO) > 0
                ? request.getTarifPersonnalise()
                : (chambre != null ? chambre.getTarifNuitee() : BigDecimal.ZERO);
        BigDecimal montantBrut = tarif.multiply(BigDecimal.valueOf(nbNuits));

        TypeRemise remiseType = Remises.parse(request.getRemiseType());
        BigDecimal remiseMontant = Remises.calculer(montantBrut, remiseType, request.getRemiseValeur());
        BigDecimal montantPrevu = montantBrut.subtract(remiseMontant);

        BigDecimal acompteRequis = request.getAcompteRequis() != null
                ? request.getAcompteRequis()
                : montantPrevu.multiply(TAUX_ACOMPTE_DEFAUT).setScale(0, RoundingMode.HALF_UP);
        if (acompteRequis.compareTo(montantPrevu) > 0) {
            acompteRequis = montantPrevu;
        }

        String numero = "RES-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Utilisateur agent = getAgentConnecte();

        Reservation reservation = Reservation.builder()
                .numeroReservation(numero)
                .client(client)
                .chambre(chambre)
                .typeChambre(request.getTypeChambre() != null ? TypeChambre.valueOf(request.getTypeChambre()) : null)
                .agent(agent)
                .dateArrivee(dateArriveDT)
                .dateDepart(dateDepartDT)
                .nbNuits((int) nbNuits)
                .montantBrut(montantBrut)
                .remiseType(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? remiseType : null)
                .remiseValeur(request.getRemiseValeur() != null ? request.getRemiseValeur() : BigDecimal.ZERO)
                .remiseMontant(remiseMontant)
                .remiseMotif(remiseMontant.compareTo(BigDecimal.ZERO) > 0 ? request.getRemiseMotif() : null)
                .montantPrevu(montantPrevu)
                .acompte(BigDecimal.ZERO)
                .acompteRequis(acompteRequis)
                .dateLimiteAnnulation(dateArriveDT.minusHours(PREAVIS_ANNULATION_HEURES))
                .penaliteAnnulation(BigDecimal.ZERO)
                .montantRembourse(BigDecimal.ZERO)
                .statut(StatutReservation.EN_ATTENTE)
                .notes(request.getNotes())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        notificationService.creer("NOUVELLE_RESERVATION", "Nouvelle réservation",
                client.getNomComplet() + " — arrivée le " + request.getDateArrivee()
                        + " · acompte attendu " + acompteRequis + " FCFA", "/reservations");

        // Acompte versé dès la prise de réservation : il confirme immédiatement.
        if (request.getAcompte() != null && request.getAcompte().compareTo(BigDecimal.ZERO) > 0) {
            AcompteRequest acompte = new AcompteRequest();
            acompte.setMontant(request.getAcompte());
            acompte.setMode(request.getModeAcompte() != null ? request.getModeAcompte() : ModePaiement.ESPECES.name());
            acompte.setReferenceTransaction(request.getReferenceAcompte());
            acompte.setCaisseId(request.getCaisseId());
            return enregistrerAcompte(saved.getId(), acompte);
        }

        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Acompte
    // ------------------------------------------------------------------

    /**
     * Encaisse l'acompte et confirme la réservation dès que le minimum exigé est
     * atteint. Les versements partiels s'additionnent : un client peut compléter
     * son acompte en plusieurs fois.
     */
    @Transactional
    public ReservationResponse enregistrerAcompte(Long id, AcompteRequest request) {
        Reservation reservation = findById(id);

        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Cette réservation est annulée");
        }
        if (reservation.getStatut() == StatutReservation.ARRIVEE) {
            throw new BusinessException("Le client est déjà arrivé : l'encaissement se fait sur le séjour");
        }
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de l'acompte doit être supérieur à zéro");
        }

        BigDecimal nouvelAcompte = reservation.getAcompte().add(request.getMontant());
        BigDecimal montantPrevu = reservation.getMontantPrevu() != null
                ? reservation.getMontantPrevu() : BigDecimal.ZERO;
        if (montantPrevu.compareTo(BigDecimal.ZERO) > 0 && nouvelAcompte.compareTo(montantPrevu) > 0) {
            throw new BusinessException("L'acompte cumulé (" + nouvelAcompte + " FCFA) dépasserait le montant "
                    + "prévu du séjour (" + montantPrevu + " FCFA)");
        }

        ModePaiement mode = CaisseService.parseEnum(ModePaiement.class, request.getMode(), "mode de règlement");
        Utilisateur agent = getAgentConnecte();
        Caisse caisse = caisseService.resoudreCaisse(request.getCaisseId());

        caisseService.enregistrer(caisse, SensMouvement.ENTREE, TypeMouvementCaisse.ACOMPTE_RESERVATION,
                request.getMontant(), mode,
                "Acompte réservation " + reservation.getNumeroReservation()
                        + " — " + reservation.getClient().getNomComplet(),
                request.getReferenceTransaction(), "RESERVATION", reservation.getId(), agent);

        reservation.setAcompte(nouvelAcompte);
        reservation.setDateAcompte(LocalDateTime.now());
        reservation.setModeAcompte(mode);
        if (request.getReferenceTransaction() != null && !request.getReferenceTransaction().isBlank()) {
            reservation.setReferenceAcompte(request.getReferenceTransaction());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            reservation.setNotes(concat(reservation.getNotes(), request.getNotes()));
        }

        boolean suffisant = nouvelAcompte.compareTo(reservation.getAcompteRequis()) >= 0;
        if (suffisant && reservation.getStatut() == StatutReservation.EN_ATTENTE) {
            reservation.setStatut(StatutReservation.CONFIRMEE);
            notificationService.creer("ARRIVEE_PREVUE", "Réservation confirmée",
                    reservation.getClient().getNomComplet() + " — arrivée le "
                            + reservation.getDateArrivee().toLocalDate()
                            + " (acompte de " + nouvelAcompte + " FCFA encaissé)", "/reservations");
        }

        auditService.log(agent, "ACOMPTE_RESERVATION", "Reservation", reservation.getId(),
                "Acompte de " + request.getMontant() + " FCFA (cumul " + nouvelAcompte + " / "
                        + reservation.getAcompteRequis() + " requis)");

        return toResponse(reservationRepository.save(reservation));
    }

    // ------------------------------------------------------------------
    // Annulation
    // ------------------------------------------------------------------

    /**
     * Simulation de l'annulation à l'instant présent, sans rien modifier. Permet
     * à l'agent d'annoncer au client ce qu'il perd avant de valider — et de lui
     * laisser la possibilité de renoncer.
     */
    @Transactional(readOnly = true)
    public PolitiqueAnnulationResponse politiqueAnnulation(Long id) {
        Reservation reservation = findById(id);
        LocalDateTime limite = limiteAnnulation(reservation);
        LocalDateTime maintenant = LocalDateTime.now();

        long heures = ChronoUnit.HOURS.between(maintenant, reservation.getDateArrivee());
        boolean dansLesDelais = !maintenant.isAfter(limite);

        BigDecimal acompte = reservation.getAcompte();
        BigDecimal penalite = dansLesDelais ? BigDecimal.ZERO : acompte;
        BigDecimal remboursement = acompte.subtract(penalite);

        String message = dansLesDelais
                ? "Annulation dans les délais : l'acompte de " + acompte + " FCFA est intégralement remboursable."
                : "Annulation à moins de " + PREAVIS_ANNULATION_HEURES + " h de l'arrivée : l'acompte de "
                    + acompte + " FCFA reste acquis à la résidence.";
        if (acompte.compareTo(BigDecimal.ZERO) == 0) {
            message = dansLesDelais
                    ? "Annulation dans les délais, aucun acompte n'a été versé."
                    : "Annulation tardive, mais aucun acompte n'avait été versé : rien n'est dû.";
        }

        return PolitiqueAnnulationResponse.builder()
                .reservationId(reservation.getId())
                .dateArrivee(reservation.getDateArrivee())
                .dateLimiteAnnulation(limite)
                .heuresAvantArrivee(heures)
                .dansLesDelais(dansLesDelais)
                .acompteVerse(acompte)
                .penalite(penalite)
                .remboursement(remboursement)
                .message(message)
                .build();
    }

    @Transactional
    public ReservationResponse annuler(Long id, AnnulationRequest request) {
        Reservation reservation = findById(id);

        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Cette réservation est déjà annulée");
        }
        if (reservation.getStatut() == StatutReservation.ARRIVEE) {
            throw new BusinessException("Le client est arrivé : la réservation ne peut plus être annulée");
        }

        PolitiqueAnnulationResponse politique = politiqueAnnulation(id);
        Utilisateur agent = getAgentConnecte();

        reservation.setStatut(StatutReservation.ANNULEE);
        reservation.setDateAnnulation(LocalDateTime.now());
        reservation.setMotifAnnulation(request != null ? request.getMotif() : null);
        reservation.setAnnulePar(agent);
        reservation.setPenaliteAnnulation(politique.getPenalite());

        // Le remboursement n'est pas automatique : l'agent peut avoir convenu
        // d'un report ou d'un avoir plutôt que d'une restitution en espèces.
        boolean rembourser = request != null && Boolean.TRUE.equals(request.getRembourser());
        if (rembourser) {
            if (politique.getRemboursement().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Aucun montant n'est remboursable sur cette réservation");
            }
            ModePaiement mode = request.getMode() != null
                    ? CaisseService.parseEnum(ModePaiement.class, request.getMode(), "mode de remboursement")
                    : (reservation.getModeAcompte() != null ? reservation.getModeAcompte() : ModePaiement.ESPECES);
            Caisse caisse = caisseService.resoudreCaisse(request.getCaisseId());

            caisseService.enregistrer(caisse, SensMouvement.SORTIE, TypeMouvementCaisse.REMBOURSEMENT,
                    politique.getRemboursement(), mode,
                    "Remboursement acompte — réservation " + reservation.getNumeroReservation()
                            + " (" + reservation.getClient().getNomComplet() + ")",
                    reservation.getReferenceAcompte(), "RESERVATION_ANNULEE", reservation.getId(), agent);

            reservation.setMontantRembourse(politique.getRemboursement());
        }

        // La chambre bloquée redevient disponible : c'est tout l'intérêt d'être
        // prévenu 48 h à l'avance, il reste le temps de la relouer.
        Reservation saved = reservationRepository.save(reservation);

        String detail = politique.isDansLesDelais()
                ? "Annulation dans les délais"
                : "Annulation tardive — pénalité de " + politique.getPenalite() + " FCFA";
        auditService.log(agent, "ANNULATION_RESERVATION", "Reservation", saved.getId(),
                detail + (request != null && request.getMotif() != null ? " — " + request.getMotif() : ""));

        notificationService.creer("RESERVATION_ANNULEE",
                politique.isDansLesDelais() ? "Réservation annulée" : "Annulation tardive",
                reservation.getClient().getNomComplet() + " — arrivée prévue le "
                        + reservation.getDateArrivee().toLocalDate()
                        + (politique.getPenalite().compareTo(BigDecimal.ZERO) > 0
                            ? " · pénalité " + politique.getPenalite() + " FCFA" : ""),
                "/reservations");

        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Statuts
    // ------------------------------------------------------------------

    @Transactional
    public ReservationResponse updateStatut(Long id, String statut) {
        Reservation reservation = findById(id);
        StatutReservation nouveauStatut = CaisseService.parseEnum(
                StatutReservation.class, statut, "statut de réservation");

        if (nouveauStatut == StatutReservation.ANNULEE) {
            return annuler(id, null);
        }

        // C'est la règle de maison : sans acompte suffisant, pas de confirmation.
        if (nouveauStatut == StatutReservation.CONFIRMEE
                && reservation.getAcompte().compareTo(reservation.getAcompteRequis()) < 0) {
            BigDecimal manque = reservation.getAcompteRequis().subtract(reservation.getAcompte());
            throw new BusinessException("La confirmation exige un acompte de "
                    + reservation.getAcompteRequis() + " FCFA — il manque " + manque + " FCFA. "
                    + "Enregistrez l'acompte du client pour confirmer la réservation.");
        }

        reservation.setStatut(nouveauStatut);
        Reservation saved = reservationRepository.save(reservation);
        if (nouveauStatut == StatutReservation.CONFIRMEE) {
            notificationService.creer("ARRIVEE_PREVUE", "Arrivée confirmée",
                    reservation.getClient().getNomComplet() + " — le " + reservation.getDateArrivee().toLocalDate(),
                    "/reservations");
        }
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAll(Pageable pageable) {
        return reservationRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findArriveesDuJour() {
        LocalDateTime debut = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fin = debut.plusDays(1);
        return reservationRepository.findArriveesPeriode(debut, fin, StatutReservation.CONFIRMEE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation", id));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Date limite d'annulation sans frais. Les réservations créées avant la mise
     * en place de la règle n'ont pas la colonne renseignée : on la déduit alors
     * de la date d'arrivée.
     */
    private LocalDateTime limiteAnnulation(Reservation r) {
        return r.getDateLimiteAnnulation() != null
                ? r.getDateLimiteAnnulation()
                : r.getDateArrivee().minusHours(PREAVIS_ANNULATION_HEURES);
    }

    private String concat(String existant, String ajout) {
        if (existant == null || existant.isBlank()) return ajout;
        return existant + "\n" + ajout;
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    public ReservationResponse toResponse(Reservation r) {
        BigDecimal acompte = r.getAcompte() != null ? r.getAcompte() : BigDecimal.ZERO;
        BigDecimal requis = r.getAcompteRequis() != null ? r.getAcompteRequis() : BigDecimal.ZERO;
        LocalDateTime limite = limiteAnnulation(r);

        return ReservationResponse.builder()
                .id(r.getId())
                .numeroReservation(r.getNumeroReservation())
                .clientId(r.getClient().getId())
                .clientNom(r.getClient().getNomComplet())
                .clientTelephone(r.getClient().getTelephone())
                .chambreId(r.getChambre() != null ? r.getChambre().getId() : null)
                .chambreNumero(r.getChambre() != null ? r.getChambre().getNumero() : null)
                .typeChambre(r.getTypeChambre() != null ? r.getTypeChambre().name() : null)
                .dateArrivee(r.getDateArrivee())
                .dateDepart(r.getDateDepart())
                .nbNuits(r.getNbNuits())
                .montantBrut(r.getMontantBrut())
                .remiseType(r.getRemiseType() != null ? r.getRemiseType().name() : null)
                .remiseValeur(r.getRemiseValeur())
                .remiseMontant(r.getRemiseMontant())
                .remiseMotif(r.getRemiseMotif())
                .montantPrevu(r.getMontantPrevu())
                .acompte(acompte)
                .acompteRequis(requis)
                .resteAcompte(requis.subtract(acompte).max(BigDecimal.ZERO))
                .acompteSuffisant(acompte.compareTo(requis) >= 0 && acompte.compareTo(BigDecimal.ZERO) > 0)
                .dateAcompte(r.getDateAcompte())
                .modeAcompte(r.getModeAcompte() != null ? r.getModeAcompte().name() : null)
                .referenceAcompte(r.getReferenceAcompte())
                .dateLimiteAnnulation(limite)
                .annulableSansFrais(!LocalDateTime.now().isAfter(limite))
                .dateAnnulation(r.getDateAnnulation())
                .motifAnnulation(r.getMotifAnnulation())
                .penaliteAnnulation(r.getPenaliteAnnulation())
                .montantRembourse(r.getMontantRembourse())
                .annuleParNom(r.getAnnulePar() != null ? r.getAnnulePar().getNomComplet() : null)
                .statut(r.getStatut().name())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
