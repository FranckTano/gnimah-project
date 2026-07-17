package com.gnimah.backend.service;

import com.gnimah.backend.dto.auth.ResoudreReinitialisationResponse;
import com.gnimah.backend.entity.PasswordResetRequest;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.PasswordResetRequestRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetRequestServiceTest {

    @Mock private PasswordResetRequestRepository passwordResetRequestRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetRequestService passwordResetRequestService;

    @Test
    void demander_withUnknownUsername_doesNotCreateRequest_andDoesNotThrow() {
        when(utilisateurRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        passwordResetRequestService.demander("inconnu");

        // Ne doit ni lever d'exception, ni révéler d'une autre façon que le compte n'existe pas.
        verify(passwordResetRequestRepository, never()).save(any());
    }

    @Test
    void demander_withInactiveAccount_doesNotCreateRequest() {
        Utilisateur inactif = Utilisateur.builder().id(2L).username("ancien").actif(false).role(Role.AGENT).build();
        when(utilisateurRepository.findByUsername("ancien")).thenReturn(Optional.of(inactif));

        passwordResetRequestService.demander("ancien");

        verify(passwordResetRequestRepository, never()).save(any());
    }

    @Test
    void demander_withActiveAccount_savesRequest() {
        Utilisateur actif = Utilisateur.builder().id(3L).username("agent").actif(true).role(Role.AGENT).build();
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.of(actif));

        passwordResetRequestService.demander("agent");

        ArgumentCaptor<PasswordResetRequest> captor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(passwordResetRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getUtilisateur()).isEqualTo(actif);
        assertThat(captor.getValue().getStatut()).isEqualTo("EN_ATTENTE");
    }

    @Test
    void resoudre_unknownId_throwsResourceNotFound() {
        when(passwordResetRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetRequestService.resoudre(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resoudre_alreadyTraitee_throwsBusinessException() {
        Utilisateur user = Utilisateur.builder().id(1L).username("agent").role(Role.AGENT).build();
        PasswordResetRequest demande = PasswordResetRequest.builder()
                .id(5L).utilisateur(user).statut("TRAITEE").build();
        when(passwordResetRequestRepository.findById(5L)).thenReturn(Optional.of(demande));

        assertThatThrownBy(() -> passwordResetRequestService.resoudre(5L))
                .isInstanceOf(BusinessException.class);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void resoudre_pending_generatesPasswordAndMarksTraitee() {
        try {
            Utilisateur admin = Utilisateur.builder().id(1L).username("admin").role(Role.ADMIN).build();
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("admin", null));

            Utilisateur agent = Utilisateur.builder().id(2L).username("agent").role(Role.AGENT).build();
            PasswordResetRequest demande = PasswordResetRequest.builder()
                    .id(5L).utilisateur(agent).statut("EN_ATTENTE").build();
            when(passwordResetRequestRepository.findById(5L)).thenReturn(Optional.of(demande));
            when(utilisateurRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
            when(passwordEncoder.encode(any())).thenReturn("$2a$12$newhash");

            ResoudreReinitialisationResponse response = passwordResetRequestService.resoudre(5L);

            assertThat(response.getUsername()).isEqualTo("agent");
            assertThat(response.getNouveauMotDePasse()).hasSize(8);
            assertThat(agent.getPassword()).isEqualTo("$2a$12$newhash");
            assertThat(demande.getStatut()).isEqualTo("TRAITEE");
            assertThat(demande.getTraitePar()).isEqualTo(admin);
            assertThat(demande.getTraiteLe()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
