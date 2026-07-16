package com.gnimah.backend.service;

import com.gnimah.backend.dto.evenement.EvenementRequest;
import com.gnimah.backend.dto.evenement.EvenementResponse;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Evenement;
import com.gnimah.backend.entity.enums.StatutEvenement;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.EvenementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    @Transactional
    public EvenementResponse create(EvenementRequest request) {
        Evenement evenement = new Evenement();
        applyRequest(evenement, request);
        evenement.setStatut(StatutEvenement.PLANIFIE);
        Evenement saved = evenementRepository.save(evenement);
        notificationService.creer("NOUVEL_EVENEMENT", "Nouvel événement",
                saved.getIntitule() + " — le " + saved.getDateDebut().toLocalDate(), "/evenements");
        return toResponse(saved);
    }

    @Transactional
    public EvenementResponse update(Long id, EvenementRequest request) {
        Evenement evenement = findById(id);
        applyRequest(evenement, request);
        return toResponse(evenementRepository.save(evenement));
    }

    @Transactional(readOnly = true)
    public List<EvenementResponse> findAll() {
        return evenementRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvenementResponse> findByMois(int annee, int mois) {
        YearMonth yearMonth = YearMonth.of(annee, mois);
        LocalDateTime debut = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime fin = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        return evenementRepository.findByPeriode(debut, fin).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public EvenementResponse updateStatut(Long id, StatutEvenement statut) {
        Evenement evenement = findById(id);
        evenement.setStatut(statut);
        return toResponse(evenementRepository.save(evenement));
    }

    @Transactional
    public void delete(Long id) {
        Evenement evenement = findById(id);
        evenementRepository.delete(evenement);
    }

    private void applyRequest(Evenement evenement, EvenementRequest request) {
        evenement.setIntitule(request.getTitre());
        evenement.setDescription(request.getDescription());
        evenement.setDateDebut(request.getDateDebut());
        evenement.setDateFin(request.getDateFin());
        evenement.setSalle(request.getLieu());
        evenement.setTypeEvenement(request.getTypeEvenement() != null ? request.getTypeEvenement() : "REUNION");
        evenement.setNbPersonnes(request.getNombreParticipants() != null ? request.getNombreParticipants() : 1);
        evenement.setMontant(request.getMontant() != null ? request.getMontant() : evenement.getMontant());
        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));
            evenement.setClient(client);
        } else {
            evenement.setClient(null);
        }
    }

    private Evenement findById(Long id) {
        return evenementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement", id));
    }

    private EvenementResponse toResponse(Evenement e) {
        return EvenementResponse.builder()
                .id(e.getId())
                .titre(e.getIntitule())
                .description(e.getDescription())
                .dateDebut(e.getDateDebut())
                .dateFin(e.getDateFin())
                .lieu(e.getSalle())
                .typeEvenement(e.getTypeEvenement())
                .statut(e.getStatut().name())
                .nombreParticipants(e.getNbPersonnes())
                .montant(e.getMontant())
                .clientId(e.getClient() != null ? e.getClient().getId() : null)
                .clientNom(e.getClient() != null ? e.getClient().getNomComplet() : null)
                .build();
    }
}
