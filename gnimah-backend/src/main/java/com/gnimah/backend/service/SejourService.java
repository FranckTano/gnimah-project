package com.gnimah.backend.service;

import com.gnimah.backend.dto.sejour.SejourRequest;
import com.gnimah.backend.dto.sejour.SejourResponse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Sejour;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.TypeLocation;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.SejourRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SejourService {

    private final SejourRepository sejourRepository;
    private final ClientRepository clientRepository;
    private final ChambreService chambreService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    @Transactional
    public SejourResponse checkIn(SejourRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));
        Chambre chambre = chambreService.findById(request.getChambreId());

        if (chambre.getEtat() != EtatChambre.LIBRE) {
            throw new BusinessException("La chambre " + chambre.getNumero() + " n'est pas disponible (état: " + chambre.getEtat() + ")");
        }

        TypeLocation typeLocation = TypeLocation.valueOf(request.getTypeLocation());

        BigDecimal montantTotal = calculerMontant(typeLocation, request, chambre);
        BigDecimal montantPaye = request.getMontantPaye() != null ? request.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal resteAPayer = montantTotal.subtract(montantPaye);

        String numeroRecu = genererNumeroRecu();

        Utilisateur agent = getAgentConnecte();

        Sejour sejour = Sejour.builder()
                .numeroRecu(numeroRecu)
                .client(client)
                .chambre(chambre)
                .agent(agent)
                .typeLocation(typeLocation)
                .dateEntree(request.getDateEntree())
                .dateSortie(request.getDateSortie())
                .heureEntree(request.getHeureEntree())
                .heureSortie(request.getHeureSortie())
                .montantTotal(montantTotal)
                .montantPaye(montantPaye)
                .resteAPayer(resteAPayer)
                .statut("EN_COURS")
                .notes(request.getNotes())
                .reservationId(request.getReservationId())
                .build();

        if (typeLocation == TypeLocation.SEJOUR && request.getDateSortie() != null) {
            long nbJours = Duration.between(request.getDateEntree(), request.getDateSortie()).toDays();
            sejour.setNbJours((int) nbJours);
        } else if (typeLocation == TypeLocation.PASSAGE && request.getDateSortie() != null) {
            long nbHeures = Duration.between(request.getDateEntree(), request.getDateSortie()).toHours();
            sejour.setNbHeures((int) nbHeures);
        }

        chambre.setEtat(EtatChambre.OCCUPEE);
        client.setNbSejours(client.getNbSejours() + 1);
        clientRepository.save(client);

        Sejour saved = sejourRepository.save(sejour);
        auditService.log(agent, "CHECK_IN", "Sejour", saved.getId(), "Check-in chambre " + chambre.getNumero() + " pour " + client.getNomComplet());

        return toResponse(saved);
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

    private BigDecimal calculerMontant(TypeLocation type, SejourRequest request, Chambre chambre) {
        if (type == TypeLocation.SEJOUR) {
            if (request.getDateSortie() != null) {
                long nbJours = Duration.between(request.getDateEntree(), request.getDateSortie()).toDays();
                if (nbJours <= 0) nbJours = 1;
                return chambre.getTarifNuitee().multiply(BigDecimal.valueOf(nbJours));
            }
            return chambre.getTarifNuitee();
        } else {
            if (request.getDateSortie() != null) {
                long nbHeures = Duration.between(request.getDateEntree(), request.getDateSortie()).toHours();
                if (nbHeures <= 0) nbHeures = 1;
                return chambre.getTarifPassage().multiply(BigDecimal.valueOf(Math.ceil(nbHeures / 3.0)));
            }
            return chambre.getTarifPassage();
        }
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
                .montantTotal(s.getMontantTotal())
                .montantPaye(s.getMontantPaye())
                .resteAPayer(s.getResteAPayer())
                .statut(s.getStatut())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
