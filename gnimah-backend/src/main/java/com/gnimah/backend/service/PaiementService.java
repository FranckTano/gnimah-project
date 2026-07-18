package com.gnimah.backend.service;

import com.gnimah.backend.dto.paiement.PaiementRequest;
import com.gnimah.backend.dto.paiement.PaiementResponse;
import com.gnimah.backend.entity.Paiement;
import com.gnimah.backend.entity.Sejour;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.PaiementRepository;
import com.gnimah.backend.repository.SejourRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final SejourRepository sejourRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PdfReceiptService pdfReceiptService;

    @Transactional
    public PaiementResponse enregistrer(PaiementRequest request) {
        Sejour sejour = sejourRepository.findById(request.getSejourId())
                .orElseThrow(() -> new ResourceNotFoundException("Séjour", request.getSejourId()));

        if (request.getMontant().compareTo(sejour.getResteAPayer()) > 0) {
            throw new BusinessException("Le montant dépasse le reste à payer (" + sejour.getResteAPayer() + " FCFA)");
        }

        Utilisateur agent = getAgentConnecte();

        Paiement paiement = Paiement.builder()
                .sejour(sejour)
                .montant(request.getMontant())
                .mode(ModePaiement.valueOf(request.getMode()))
                .referenceTransaction(request.getReferenceTransaction())
                .agent(agent)
                .notes(request.getNotes())
                .build();

        paiementRepository.save(paiement);

        sejour.setMontantPaye(sejour.getMontantPaye().add(request.getMontant()));
        sejour.setResteAPayer(sejour.getMontantTotal().subtract(sejour.getMontantPaye()));
        sejourRepository.save(sejour);

        return toResponse(paiement);
    }

    @Transactional(readOnly = true)
    public List<PaiementResponse> findBySejour(Long sejourId) {
        return paiementRepository.findBySejourId(sejourId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] generateRecuPdf(Long id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
        return pdfReceiptService.generateReceipt(paiement);
    }

    private Utilisateur getAgentConnecte() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByUsername(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<PaiementResponse> findAll(Pageable pageable) {
        return paiementRepository.findAll(pageable).map(this::toResponse);
    }

    public PaiementResponse toResponse(Paiement p) {
        return PaiementResponse.builder()
                .id(p.getId())
                .sejourId(p.getSejour().getId())
                .clientNom(p.getSejour().getClient().getNomComplet())
                .numeroChambre(p.getSejour().getChambre().getNumero())
                .numeroRecu(p.getSejour().getNumeroRecu())
                .montant(p.getMontant())
                .modePaiement(p.getMode().name())
                .referenceTransaction(p.getReferenceTransaction())
                .dateHeure(p.getDatePaiement())
                .agentNom(p.getAgent() != null ? p.getAgent().getNomComplet() : null)
                .notes(p.getNotes())
                .build();
    }
}
