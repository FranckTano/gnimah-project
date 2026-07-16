package com.gnimah.backend.service;

import com.gnimah.backend.dto.utilisateur.UtilisateurRequest;
import com.gnimah.backend.dto.utilisateur.UtilisateurResponse;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> findAll() {
        return utilisateurRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UtilisateurResponse create(UtilisateurRequest request) {
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Ce nom d'utilisateur existe déjà");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Le mot de passe est requis à la création");
        }
        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole()))
                .actif(request.isActif())
                .telephone(request.getTelephone())
                .build();
        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurResponse update(Long id, UtilisateurRequest request) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setRole(Role.valueOf(request.getRole()));
        utilisateur.setActif(request.isActif());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurResponse toggleActif(Long id) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setActif(!utilisateur.isActif());
        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        Utilisateur utilisateur = findById(id);
        utilisateur.setPassword(passwordEncoder.encode(newPassword));
        utilisateurRepository.save(utilisateur);
    }

    private Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    private UtilisateurResponse toResponse(Utilisateur u) {
        return UtilisateurResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .email(u.getEmail())
                .telephone(u.getTelephone())
                .role(u.getRole().name())
                .actif(u.isActif())
                .dateCreation(u.getCreatedAt())
                .build();
    }
}
