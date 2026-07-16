package com.gnimah.backend.service;

import com.gnimah.backend.dto.entretien.TacheEntretienRequest;
import com.gnimah.backend.dto.entretien.TacheEntretienResponse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Evenement;
import com.gnimah.backend.entity.TacheEntretien;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.StatutTache;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ChambreRepository;
import com.gnimah.backend.repository.EvenementRepository;
import com.gnimah.backend.repository.TacheEntretienRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TacheEntretienService {

    private static final int PRIORITE_URGENTE = 3;
    private static final int PRIORITE_NORMALE = 2;

    private final TacheEntretienRepository tacheRepository;
    private final ChambreRepository chambreRepository;
    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public TacheEntretienResponse create(TacheEntretienRequest request) {
        Utilisateur agent = utilisateurRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.getAgentId()));

        TacheEntretien.TacheEntretienBuilder builder = TacheEntretien.builder()
                .titre(request.getTitre())
                .typeTache(request.getType())
                .description(request.getDescription())
                .salle(request.getSalle())
                .agent(agent)
                .superviseur(getUtilisateurConnecte())
                .statut(StatutTache.A_FAIRE)
                .dateLimite(request.getDateLimite())
                .priorite("URGENTE".equalsIgnoreCase(request.getPriorite()) ? PRIORITE_URGENTE : PRIORITE_NORMALE);

        if (request.getChambreId() != null) {
            Chambre chambre = chambreRepository.findById(request.getChambreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chambre", request.getChambreId()));
            builder.chambre(chambre);
        }
        if (request.getEvenementId() != null) {
            Evenement evenement = evenementRepository.findById(request.getEvenementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Événement", request.getEvenementId()));
            builder.evenement(evenement);
        }

        return toResponse(tacheRepository.save(builder.build()));
    }

    @Transactional(readOnly = true)
    public List<TacheEntretienResponse> findAll(StatutTache statut) {
        List<TacheEntretien> list = statut != null ? tacheRepository.findByStatut(statut) : tacheRepository.findAll();
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheEntretienResponse> findEnAttente() {
        return tacheRepository.findByStatut(StatutTache.A_FAIRE).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheEntretienResponse> findByChambre(Long chambreId) {
        return tacheRepository.findByChambreId(chambreId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public TacheEntretienResponse updateStatut(Long id, StatutTache statut) {
        TacheEntretien tache = findById(id);
        tache.setStatut(statut);
        if (statut == StatutTache.EN_COURS && tache.getDateDebut() == null) {
            tache.setDateDebut(LocalDateTime.now());
        } else if (statut == StatutTache.TERMINE) {
            tache.setDateFin(LocalDateTime.now());
        }
        return toResponse(tacheRepository.save(tache));
    }

    @Transactional
    public void delete(Long id) {
        tacheRepository.delete(findById(id));
    }

    private Utilisateur getUtilisateurConnecte() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByUsername(username).orElse(null);
    }

    private TacheEntretien findById(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche", id));
    }

    private TacheEntretienResponse toResponse(TacheEntretien t) {
        return TacheEntretienResponse.builder()
                .id(t.getId())
                .titre(t.getTitre())
                .chambreId(t.getChambre() != null ? t.getChambre().getId() : null)
                .chambreNumero(t.getChambre() != null ? t.getChambre().getNumero() : null)
                .salle(t.getSalle())
                .evenementId(t.getEvenement() != null ? t.getEvenement().getId() : null)
                .evenementTitre(t.getEvenement() != null ? t.getEvenement().getIntitule() : null)
                .type(t.getTypeTache())
                .description(t.getDescription())
                .statut(t.getStatut().name())
                .priorite(t.getPriorite() >= PRIORITE_URGENTE ? "URGENTE" : "NORMALE")
                .agentId(t.getAgent() != null ? t.getAgent().getId() : null)
                .assigneA(t.getAgent() != null ? t.getAgent().getNomComplet() : null)
                .responsableId(t.getSuperviseur() != null ? t.getSuperviseur().getId() : null)
                .responsableNom(t.getSuperviseur() != null ? t.getSuperviseur().getNomComplet() : null)
                .dateLimite(t.getDateLimite())
                .dateCreation(t.getCreatedAt())
                .dateFin(t.getDateFin())
                .build();
    }
}
