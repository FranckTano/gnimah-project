package com.gnimah.backend.service;

import com.gnimah.backend.dto.reservation.ReservationRequest;
import com.gnimah.backend.dto.reservation.ReservationResponse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeChambre;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ClientService clientService;
    private final ChambreService chambreService;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Client client = clientService.findById(request.getClientId());
        Chambre chambre = null;

        // Le frontend envoie des dates "yyyy-MM-dd" (LocalDate) — on convertit en LocalDateTime pour l'entité
        LocalDateTime dateArriveDT = request.getDateArrivee().atStartOfDay();
        LocalDateTime dateDepartDT = request.getDateDepart().atStartOfDay();

        if (request.getChambreId() != null) {
            chambre = chambreService.findById(request.getChambreId());
            List<Reservation> conflits = reservationRepository.findConflits(
                    request.getChambreId(), dateArriveDT, dateDepartDT);
            if (!conflits.isEmpty()) {
                throw new BusinessException("La chambre est déjà réservée pour cette période");
            }
        }

        long nbNuits = ChronoUnit.DAYS.between(request.getDateArrivee(), request.getDateDepart());
        BigDecimal montantPrevu = chambre != null
                ? chambre.getTarifNuitee().multiply(BigDecimal.valueOf(Math.max(nbNuits, 1)))
                : BigDecimal.ZERO;

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
                .nbNuits((int) Math.max(nbNuits, 1))
                .montantPrevu(montantPrevu)
                .acompte(request.getAcompte() != null ? request.getAcompte() : BigDecimal.ZERO)
                .statut(StatutReservation.EN_ATTENTE)
                .notes(request.getNotes())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        notificationService.creer("NOUVELLE_RESERVATION", "Nouvelle réservation",
                client.getNomComplet() + " — arrivée le " + request.getDateArrivee(), "/reservations");
        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse updateStatut(Long id, String statut) {
        Reservation reservation = findById(id);
        StatutReservation nouveauStatut = StatutReservation.valueOf(statut);
        reservation.setStatut(nouveauStatut);
        Reservation saved = reservationRepository.save(reservation);
        if (nouveauStatut == StatutReservation.CONFIRMEE) {
            notificationService.creer("ARRIVEE_PREVUE", "Arrivée confirmée",
                    reservation.getClient().getNomComplet() + " — le " + reservation.getDateArrivee().toLocalDate(), "/reservations");
        }
        return toResponse(saved);
    }

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

    private Utilisateur getAgentConnecte() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByUsername(username).orElse(null);
    }

    public ReservationResponse toResponse(Reservation r) {
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
                .montantPrevu(r.getMontantPrevu())
                .acompte(r.getAcompte())
                .statut(r.getStatut().name())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
