package com.gnimah.backend.security;

import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));

        if (!utilisateur.isActif()) {
            throw new UsernameNotFoundException("Compte désactivé: " + username);
        }

        return User.builder()
                .username(utilisateur.getUsername())
                .password(utilisateur.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())))
                .build();
    }
}
