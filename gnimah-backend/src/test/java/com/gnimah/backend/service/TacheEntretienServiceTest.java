package com.gnimah.backend.service;

import com.gnimah.backend.dto.entretien.TacheEntretienRequest;
import com.gnimah.backend.dto.entretien.TacheEntretienResponse;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.ChambreRepository;
import com.gnimah.backend.repository.EvenementRepository;
import com.gnimah.backend.repository.TacheEntretienRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TacheEntretienServiceTest {

    @Mock private TacheEntretienRepository tacheRepository;
    @Mock private ChambreRepository chambreRepository;
    @Mock private EvenementRepository evenementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private TacheEntretienService tacheEntretienService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private TacheEntretienRequest baseRequest() {
        TacheEntretienRequest request = new TacheEntretienRequest();
        request.setTitre("Nettoyage chambre 101");
        request.setType("NETTOYAGE");
        request.setDescription("Après check-out");
        request.setAgentId(10L);
        request.setPriorite("NORMALE");
        return request;
    }

    @Test
    void create_withUnknownAgent_throwsResourceNotFound() {
        TacheEntretienRequest request = baseRequest();
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tacheEntretienService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tacheRepository, never()).save(any());
    }

    @Test
    void create_withoutTarget_savesTaskWithNoChambreSalleOrEvenement() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("responsable", null));
        TacheEntretienRequest request = baseRequest();

        Utilisateur agent = Utilisateur.builder().id(10L).nom("Réception").prenom("Agent").role(Role.AGENT).build();
        Utilisateur responsable = Utilisateur.builder().id(20L).nom("GNIMAH").prenom("Responsable").role(Role.RESPONSABLE).build();
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(utilisateurRepository.findByUsername("responsable")).thenReturn(Optional.of(responsable));
        when(tacheRepository.save(any())).thenAnswer(inv -> {
            var t = inv.getArgument(0, com.gnimah.backend.entity.TacheEntretien.class);
            t.setId(1L);
            return t;
        });

        TacheEntretienResponse response = tacheEntretienService.create(request);

        assertThat(response.getChambreId()).isNull();
        assertThat(response.getSalle()).isNull();
        assertThat(response.getEvenementId()).isNull();
        assertThat(response.getAgentId()).isEqualTo(10L);
        assertThat(response.getResponsableNom()).isEqualTo("GNIMAH Responsable");
        verifyNoInteractions(chambreRepository, evenementRepository);
    }

    @Test
    void create_withChambreTarget_resolvesChambre() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("directeur", null));
        TacheEntretienRequest request = baseRequest();
        request.setChambreId(101L);

        Utilisateur agent = Utilisateur.builder().id(10L).nom("Réception").prenom("Agent").role(Role.AGENT).build();
        Chambre chambre = Chambre.builder().id(101L).numero("101").build();
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(chambreRepository.findById(101L)).thenReturn(Optional.of(chambre));
        when(utilisateurRepository.findByUsername("directeur")).thenReturn(Optional.empty());

        ArgumentCaptor<com.gnimah.backend.entity.TacheEntretien> captor =
                ArgumentCaptor.forClass(com.gnimah.backend.entity.TacheEntretien.class);
        when(tacheRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        TacheEntretienResponse response = tacheEntretienService.create(request);

        assertThat(captor.getValue().getChambre()).isEqualTo(chambre);
        assertThat(response.getChambreNumero()).isEqualTo("101");
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void create_withUnknownChambre_throwsResourceNotFound() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("directeur", null));
        TacheEntretienRequest request = baseRequest();
        request.setChambreId(999L);

        Utilisateur agent = Utilisateur.builder().id(10L).role(Role.AGENT).build();
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(chambreRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tacheEntretienService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tacheRepository, never()).save(any());
    }

    @Test
    void create_priorityUrgente_mapsToIntegerThree() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("responsable", null));
        TacheEntretienRequest request = baseRequest();
        request.setPriorite("URGENTE");

        Utilisateur agent = Utilisateur.builder().id(10L).role(Role.AGENT).build();
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(utilisateurRepository.findByUsername("responsable")).thenReturn(Optional.empty());

        ArgumentCaptor<com.gnimah.backend.entity.TacheEntretien> captor =
                ArgumentCaptor.forClass(com.gnimah.backend.entity.TacheEntretien.class);
        when(tacheRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        TacheEntretienResponse response = tacheEntretienService.create(request);

        assertThat(captor.getValue().getPriorite()).isEqualTo(3);
        assertThat(response.getPriorite()).isEqualTo("URGENTE");
    }
}
