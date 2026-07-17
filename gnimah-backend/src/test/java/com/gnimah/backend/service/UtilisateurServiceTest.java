package com.gnimah.backend.service;

import com.gnimah.backend.dto.utilisateur.UtilisateurRequest;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UtilisateurService utilisateurService;

    private UtilisateurRequest validRequest() {
        UtilisateurRequest request = new UtilisateurRequest();
        request.setUsername("nouveau");
        request.setPassword("MotDePasse@2026");
        request.setNom("Test");
        request.setPrenom("Utilisateur");
        request.setEmail("test@gnimah.com");
        request.setRole("AGENT");
        request.setActif(true);
        return request;
    }

    @Test
    void create_withDuplicateUsername_throwsBusinessException() {
        UtilisateurRequest request = validRequest();
        when(utilisateurRepository.existsByUsername("nouveau")).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void create_withoutPassword_throwsBusinessException() {
        UtilisateurRequest request = validRequest();
        request.setPassword(null);
        when(utilisateurRepository.existsByUsername("nouveau")).thenReturn(false);

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requis");
    }

    @Test
    void create_withWeakPassword_throwsBusinessException() {
        UtilisateurRequest request = validRequest();
        request.setPassword("short1");
        when(utilisateurRepository.existsByUsername("nouveau")).thenReturn(false);

        assertThatThrownBy(() -> utilisateurService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8 caractères");

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void create_withValidRequest_encodesPasswordAndSaves() {
        UtilisateurRequest request = validRequest();
        when(utilisateurRepository.existsByUsername("nouveau")).thenReturn(false);
        when(passwordEncoder.encode("MotDePasse@2026")).thenReturn("$2a$12$encoded");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        var response = utilisateurService.create(request);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$12$encoded");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.AGENT);
        assertThat(response.getUsername()).isEqualTo("nouveau");
    }

    @Test
    void update_withBlankPassword_keepsExistingPassword() {
        Utilisateur existing = Utilisateur.builder()
                .id(1L).username("agent").nom("Old").prenom("Name")
                .password("$2a$12$oldhash").role(Role.AGENT).actif(true).build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurRequest request = validRequest();
        request.setPassword("");
        request.setNom("Updated");

        utilisateurService.update(1L, request);

        assertThat(existing.getPassword()).isEqualTo("$2a$12$oldhash");
        assertThat(existing.getNom()).isEqualTo("Updated");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void update_withWeakNewPassword_throwsAndDoesNotSave() {
        Utilisateur existing = Utilisateur.builder()
                .id(1L).username("agent").nom("Old").prenom("Name")
                .password("$2a$12$oldhash").role(Role.AGENT).actif(true).build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(existing));

        UtilisateurRequest request = validRequest();
        request.setPassword("weak");

        assertThatThrownBy(() -> utilisateurService.update(1L, request))
                .isInstanceOf(BusinessException.class);

        verify(utilisateurRepository, never()).save(any());
    }
}
