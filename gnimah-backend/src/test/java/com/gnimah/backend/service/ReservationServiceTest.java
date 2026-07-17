package com.gnimah.backend.service;

import com.gnimah.backend.dto.reservation.ReservationRequest;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.ReservationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ClientService clientService;
    @Mock private ChambreService chambreService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ReservationService reservationService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_whenChambreAlreadyBookedForPeriod_throwsBusinessException() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        ReservationRequest request = new ReservationRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setDateArrivee(LocalDateTime.now().plusDays(1));
        request.setDateDepart(LocalDateTime.now().plusDays(3));

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").type(TypeChambre.STANDARD)
                .tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientService.findById(1L)).thenReturn(client);
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(reservationRepository.findConflits(eq(10L), any(), any())).thenReturn(List.of(new Reservation()));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà réservée");

        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void create_withAvailableChambre_computesMontantAndNotifies() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        ReservationRequest request = new ReservationRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setDateArrivee(LocalDateTime.now().plusDays(1));
        request.setDateDepart(LocalDateTime.now().plusDays(3));

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").type(TypeChambre.STANDARD)
                .tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientService.findById(1L)).thenReturn(client);
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(reservationRepository.findConflits(eq(10L), any(), any())).thenReturn(List.of());
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.create(request);

        // 2 nuits * 15000
        assertThat(response.getMontantPrevu()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(response.getStatut()).isEqualTo("EN_ATTENTE");
        verify(notificationService).creer(eq("NOUVELLE_RESERVATION"), anyString(), anyString(), anyString());
    }

    @Test
    void updateStatut_toConfirmee_triggersArriveePrevueNotification() {
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(1))
                .dateDepart(LocalDateTime.now().plusDays(2))
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        reservationService.updateStatut(1L, "CONFIRMEE");

        verify(notificationService).creer(eq("ARRIVEE_PREVUE"), anyString(), anyString(), anyString());
    }

    @Test
    void updateStatut_toAnnulee_doesNotTriggerNotification() {
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(1))
                .dateDepart(LocalDateTime.now().plusDays(2))
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        reservationService.updateStatut(1L, "ANNULEE");

        verifyNoInteractions(notificationService);
    }
}
