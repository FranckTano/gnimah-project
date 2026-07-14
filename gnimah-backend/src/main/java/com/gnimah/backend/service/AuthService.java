package com.gnimah.backend.service;

import com.gnimah.backend.dto.auth.*;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.UtilisateurRepository;
import com.gnimah.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        Utilisateur utilisateur = utilisateurRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", null));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", utilisateur.getRole().name());
        claims.put("userId", utilisateur.getId());

        String token = jwtUtil.generateToken(request.getUsername(), claims);

        utilisateurRepository.updateLastLogin(utilisateur.getId(), LocalDateTime.now());
        auditService.log(utilisateur, "LOGIN", "Utilisateur", utilisateur.getId(), "Connexion réussie");

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(utilisateur.getId())
                .username(utilisateur.getUsername())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().name())
                .email(utilisateur.getEmail())
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getPassword())) {
            throw new BusinessException("Ancien mot de passe incorrect");
        }

        utilisateur.setPassword(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
        auditService.log(utilisateur, "CHANGE_PASSWORD", "Utilisateur", utilisateur.getId(), "Mot de passe modifié");
    }
}
