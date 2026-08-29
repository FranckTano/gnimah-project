package com.gnimah.backend.service;

import com.gnimah.backend.dto.compte.CompteFinancierRequest;
import com.gnimah.backend.dto.compte.CompteFinancierResponse;
import com.gnimah.backend.entity.CompteFinancier;
import com.gnimah.backend.entity.enums.TypeCompteFinancier;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.CompteFinancierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Comptes bancaires et numéros mobile money destinataires des versements. */
@Service
@RequiredArgsConstructor
public class CompteFinancierService {

    private final CompteFinancierRepository compteRepository;

    @Transactional(readOnly = true)
    public List<CompteFinancierResponse> findAll() {
        return compteRepository.findByActifTrueOrderByLibelleAsc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CompteFinancierResponse create(CompteFinancierRequest request) {
        TypeCompteFinancier type = parseType(request.getType());
        validerIdentifiant(type, request);
        CompteFinancier c = CompteFinancier.builder()
                .libelle(request.getLibelle())
                .type(type)
                .etablissement(request.getEtablissement())
                .numeroCompte(request.getNumeroCompte())
                .numeroTelephone(request.getNumeroTelephone())
                .titulaire(request.getTitulaire())
                .notes(request.getNotes())
                .actif(request.getActif() == null || request.getActif())
                .build();
        return toResponse(compteRepository.save(c));
    }

    @Transactional
    public CompteFinancierResponse update(Long id, CompteFinancierRequest request) {
        CompteFinancier c = findById(id);
        TypeCompteFinancier type = parseType(request.getType());
        validerIdentifiant(type, request);
        c.setLibelle(request.getLibelle());
        c.setType(type);
        c.setEtablissement(request.getEtablissement());
        c.setNumeroCompte(request.getNumeroCompte());
        c.setNumeroTelephone(request.getNumeroTelephone());
        c.setTitulaire(request.getTitulaire());
        c.setNotes(request.getNotes());
        if (request.getActif() != null) c.setActif(request.getActif());
        return toResponse(compteRepository.save(c));
    }

    @Transactional
    public void desactiver(Long id) {
        CompteFinancier c = findById(id);
        c.setActif(false);
        compteRepository.save(c);
    }

    public CompteFinancier findById(Long id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", id));
    }

    /**
     * Un versement sans destinataire identifiable est irrécupérable : mieux vaut
     * refuser la création du compte que découvrir le problème au moment du dépôt.
     */
    private void validerIdentifiant(TypeCompteFinancier type, CompteFinancierRequest request) {
        if (type == TypeCompteFinancier.MOBILE_MONEY) {
            if (estVide(request.getNumeroTelephone())) {
                throw new BusinessException("Un compte mobile money exige un numéro de téléphone");
            }
        } else if (estVide(request.getNumeroCompte())) {
            throw new BusinessException("Un compte bancaire exige un numéro de compte");
        }
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }

    private TypeCompteFinancier parseType(String value) {
        if (value == null || value.isBlank()) return TypeCompteFinancier.BANQUE;
        return CaisseService.parseEnum(TypeCompteFinancier.class, value, "type de compte");
    }

    public CompteFinancierResponse toResponse(CompteFinancier c) {
        return CompteFinancierResponse.builder()
                .id(c.getId())
                .libelle(c.getLibelle())
                .type(c.getType().name())
                .etablissement(c.getEtablissement())
                .numeroCompte(c.getNumeroCompte())
                .numeroTelephone(c.getNumeroTelephone())
                .identifiant(c.getIdentifiant())
                .titulaire(c.getTitulaire())
                .solde(c.getSolde())
                .actif(c.isActif())
                .notes(c.getNotes())
                .build();
    }
}
