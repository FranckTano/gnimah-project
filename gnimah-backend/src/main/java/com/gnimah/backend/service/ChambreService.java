package com.gnimah.backend.service;

import com.gnimah.backend.dto.chambre.ChambreRequest;
import com.gnimah.backend.dto.chambre.ChambreResponse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ChambreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChambreService {

    private final ChambreRepository chambreRepository;

    @Transactional
    public ChambreResponse create(ChambreRequest request) {
        if (chambreRepository.findByNumero(request.getNumero()).isPresent()) {
            throw new BusinessException("La chambre " + request.getNumero() + " existe déjà");
        }
        Chambre chambre = Chambre.builder()
                .numero(request.getNumero())
                .type(TypeChambre.valueOf(request.getType()))
                .capacite(request.getCapacite())
                .tarifPassage(request.getTarifPassage())
                .tarifNuitee(request.getTarifNuitee())
                .etat(request.getEtat() != null ? EtatChambre.valueOf(request.getEtat()) : EtatChambre.LIBRE)
                .etage(request.getEtage())
                .description(request.getDescription())
                .equipements(request.getEquipements())
                .vue(request.getVue())
                .observations(request.getObservations())
                .photos(request.getPhotos())
                .build();
        return toResponse(chambreRepository.save(chambre));
    }

    @Transactional
    public ChambreResponse update(Long id, ChambreRequest request) {
        Chambre chambre = findById(id);
        chambre.setNumero(request.getNumero());
        chambre.setType(TypeChambre.valueOf(request.getType()));
        chambre.setCapacite(request.getCapacite());
        chambre.setTarifPassage(request.getTarifPassage());
        chambre.setTarifNuitee(request.getTarifNuitee());
        if (request.getEtat() != null) chambre.setEtat(EtatChambre.valueOf(request.getEtat()));
        chambre.setEtage(request.getEtage());
        chambre.setDescription(request.getDescription());
        chambre.setEquipements(request.getEquipements());
        chambre.setVue(request.getVue());
        chambre.setObservations(request.getObservations());
        chambre.setPhotos(request.getPhotos());
        return toResponse(chambreRepository.save(chambre));
    }

    @Transactional
    public ChambreResponse updateEtat(Long id, String etat) {
        Chambre chambre = findById(id);
        chambre.setEtat(EtatChambre.valueOf(etat));
        return toResponse(chambreRepository.save(chambre));
    }

    /** Soft delete / restore — rooms are referenced by historical séjours & réservations, so they're deactivated, never hard-deleted. */
    @Transactional
    public ChambreResponse toggleActif(Long id) {
        Chambre chambre = findById(id);
        chambre.setActif(!chambre.isActif());
        return toResponse(chambreRepository.save(chambre));
    }

    @Transactional(readOnly = true)
    public List<ChambreResponse> findAll() {
        return chambreRepository.findByActifTrue().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /** Includes deactivated rooms — for the room-management screen only (findAll() feeds the live room board). */
    @Transactional(readOnly = true)
    public List<ChambreResponse> findAllAdmin() {
        return chambreRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChambreResponse> findDisponibles() {
        return chambreRepository.findByEtatAndActifTrue(EtatChambre.LIBRE).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChambreResponse> findDisponiblesPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return chambreRepository.findChambresDisponibles(dateDebut, dateFin).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChambreResponse getById(Long id) {
        return toResponse(findById(id));
    }

    public Chambre findById(Long id) {
        return chambreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre", id));
    }

    public ChambreResponse toResponse(Chambre c) {
        return ChambreResponse.builder()
                .id(c.getId())
                .numero(c.getNumero())
                .type(c.getType().name())
                .capacite(c.getCapacite())
                .tarifPassage(c.getTarifPassage())
                .tarifNuitee(c.getTarifNuitee())
                .etat(c.getEtat().name())
                .etage(c.getEtage())
                .description(c.getDescription())
                .equipements(c.getEquipements())
                .vue(c.getVue())
                .observations(c.getObservations())
                .photos(c.getPhotos())
                .actif(c.isActif())
                .disponible(c.isDisponible())
                .build();
    }
}
