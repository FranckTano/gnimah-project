package com.gnimah.backend.service;

import com.gnimah.backend.dto.fournisseur.FournisseurRequest;
import com.gnimah.backend.dto.fournisseur.FournisseurResponse;
import com.gnimah.backend.entity.Fournisseur;
import com.gnimah.backend.entity.enums.TypeFournisseur;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;

    @Transactional(readOnly = true)
    public List<FournisseurResponse> findAll() {
        return fournisseurRepository.findByActifTrueOrderByNomAsc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FournisseurResponse> findByType(String type) {
        TypeFournisseur t = CaisseService.parseEnum(TypeFournisseur.class, type, "type de fournisseur");
        return fournisseurRepository.findByTypeAndActifTrueOrderByNomAsc(t).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public FournisseurResponse create(FournisseurRequest request) {
        Fournisseur f = Fournisseur.builder()
                .nom(request.getNom())
                .type(parseType(request.getType()))
                .contact(request.getContact())
                .telephone(request.getTelephone())
                .email(request.getEmail())
                .adresse(request.getAdresse())
                .notes(request.getNotes())
                .actif(request.getActif() == null || request.getActif())
                .build();
        return toResponse(fournisseurRepository.save(f));
    }

    @Transactional
    public FournisseurResponse update(Long id, FournisseurRequest request) {
        Fournisseur f = findById(id);
        f.setNom(request.getNom());
        f.setType(parseType(request.getType()));
        f.setContact(request.getContact());
        f.setTelephone(request.getTelephone());
        f.setEmail(request.getEmail());
        f.setAdresse(request.getAdresse());
        f.setNotes(request.getNotes());
        if (request.getActif() != null) f.setActif(request.getActif());
        return toResponse(fournisseurRepository.save(f));
    }

    /**
     * Désactivation plutôt que suppression : les dépenses déjà enregistrées
     * pointent vers le fournisseur et doivent rester lisibles.
     */
    @Transactional
    public void desactiver(Long id) {
        Fournisseur f = findById(id);
        f.setActif(false);
        fournisseurRepository.save(f);
    }

    public Fournisseur findById(Long id) {
        return fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
    }

    private TypeFournisseur parseType(String value) {
        if (value == null || value.isBlank()) return TypeFournisseur.DIVERS;
        return CaisseService.parseEnum(TypeFournisseur.class, value, "type de fournisseur");
    }

    public FournisseurResponse toResponse(Fournisseur f) {
        return FournisseurResponse.builder()
                .id(f.getId())
                .nom(f.getNom())
                .type(f.getType().name())
                .contact(f.getContact())
                .telephone(f.getTelephone())
                .email(f.getEmail())
                .adresse(f.getAdresse())
                .notes(f.getNotes())
                .actif(f.isActif())
                .build();
    }
}
