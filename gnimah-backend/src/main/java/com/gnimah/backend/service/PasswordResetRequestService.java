package com.gnimah.backend.service;

import com.gnimah.backend.dto.auth.PasswordResetRequestResponse;
import com.gnimah.backend.dto.auth.ResoudreReinitialisationResponse;
import com.gnimah.backend.entity.PasswordResetRequest;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.PasswordResetRequestRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PasswordResetRequestService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void demander(String username) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        if (utilisateur == null || !utilisateur.isActif()) {
            return;
        }
        PasswordResetRequest demande = PasswordResetRequest.builder()
                .utilisateur(utilisateur)
                .statut("EN_ATTENTE")
                .build();
        passwordResetRequestRepository.save(demande);
    }

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> findEnAttente() {
        return passwordResetRequestRepository.findByStatutOrderByCreatedAtDesc("EN_ATTENTE")
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ResoudreReinitialisationResponse resoudre(Long id) {
        PasswordResetRequest demande = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de réinitialisation", id));
        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Cette demande a déjà été traitée");
        }

        String nouveauMotDePasse = genererMotDePasse();
        Utilisateur utilisateur = demande.getUtilisateur();
        utilisateur.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        demande.setStatut("TRAITEE");
        demande.setTraiteLe(LocalDateTime.now());
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        utilisateurRepository.findByUsername(adminUsername).ifPresent(demande::setTraitePar);
        passwordResetRequestRepository.save(demande);

        return ResoudreReinitialisationResponse.builder()
                .username(utilisateur.getUsername())
                .nouveauMotDePasse(nouveauMotDePasse)
                .build();
    }

    private String genererMotDePasse() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private PasswordResetRequestResponse toResponse(PasswordResetRequest r) {
        return PasswordResetRequestResponse.builder()
                .id(r.getId())
                .utilisateurId(r.getUtilisateur().getId())
                .utilisateurNomComplet(r.getUtilisateur().getNomComplet())
                .username(r.getUtilisateur().getUsername())
                .statut(r.getStatut())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
