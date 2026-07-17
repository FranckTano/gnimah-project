package com.gnimah.backend.service;

import com.gnimah.backend.dto.sejour.SejourRequest;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Sejour;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.ClientRepository;
import com.gnimah.backend.repository.SejourRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SejourServiceTest {

    @Mock private SejourRepository sejourRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ChambreService chambreService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SejourService sejourService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkIn_whenChambreNotLibre_throwsBusinessException() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        SejourRequest request = new SejourRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setTypeLocation("SEJOUR");
        request.setDateEntree(LocalDateTime.now());
        request.setDateSortie(LocalDateTime.now().plusDays(2));

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);

        assertThatThrownBy(() -> sejourService.checkIn(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("101");

        verify(sejourRepository, never()).save(any());
    }

    @Test
    void checkIn_forSejourType_computesMontantAndOccupiesRoom() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        SejourRequest request = new SejourRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setTypeLocation("SEJOUR");
        LocalDateTime entree = LocalDateTime.now();
        request.setDateEntree(entree);
        request.setDateSortie(entree.plusDays(3));
        request.setMontantPaye(BigDecimal.valueOf(20000));

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(2).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = sejourService.checkIn(request);

        // 3 nuits * 15000
        assertThat(response.getMontantTotal()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(response.getResteAPayer()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(chambre.getEtat()).isEqualTo(EtatChambre.OCCUPEE);
        assertThat(client.getNbSejours()).isEqualTo(3);
    }

    @Test
    void checkOut_whenNotEnCours_throwsBusinessException() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Sejour sejour = Sejour.builder().id(1L).statut("TERMINE")
                .chambre(Chambre.builder().id(10L).numero("101").build())
                .client(Client.builder().id(1L).civilite("M.").nom("DIALLO").build())
                .build();
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejour));

        assertThatThrownBy(() -> sejourService.checkOut(1L))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(notificationService);
    }

    @Test
    void checkOut_whenEnCours_freesRoomAndNotifies() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).build();
        Sejour sejour = Sejour.builder().id(1L).statut("EN_COURS")
                .typeLocation(com.gnimah.backend.entity.enums.TypeLocation.SEJOUR)
                .chambre(chambre)
                .client(Client.builder().id(1L).civilite("M.").nom("DIALLO").build())
                .build();
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejour));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        sejourService.checkOut(1L);

        assertThat(chambre.getEtat()).isEqualTo(EtatChambre.A_NETTOYER);
        assertThat(sejour.getStatut()).isEqualTo("TERMINE");
        verify(notificationService).creer(eq("FIN_SEJOUR"), anyString(), anyString(), anyString());
    }
}
