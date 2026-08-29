package com.gnimah.backend.service;

import com.gnimah.backend.dto.reservation.AcompteRequest;
import com.gnimah.backend.dto.reservation.AnnulationRequest;
import com.gnimah.backend.dto.reservation.ReservationRequest;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.StatutReservation;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
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
import java.time.LocalDate;
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
    // L'acompte et le remboursement passent par la caisse, les gestes sensibles
    // par le journal d'audit : les deux doivent être simulés.
    @Mock private CaisseService caisseService;
    @Mock private AuditService auditService;

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
        request.setDateArrivee(LocalDate.now().plusDays(1));
        request.setDateDepart(LocalDate.now().plusDays(3));

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
        request.setDateArrivee(LocalDate.now().plusDays(1));
        request.setDateDepart(LocalDate.now().plusDays(3));

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
        // Sans acompte encaissé, la réservation reste en attente : c'est la règle
        // de maison — une réservation n'est ferme que quand l'argent est reçu.
        assertThat(response.getStatut()).isEqualTo("EN_ATTENTE");
        // Acompte par défaut : 30 % du montant prévu.
        assertThat(response.getAcompteRequis()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(response.isAcompteSuffisant()).isFalse();
        // Le préavis d'annulation est figé à la création, 48 h avant l'arrivée.
        assertThat(response.getDateLimiteAnnulation())
                .isEqualTo(response.getDateArrivee().minusHours(ReservationService.PREAVIS_ANNULATION_HEURES));
        verify(notificationService).creer(eq("NOUVELLE_RESERVATION"), anyString(), anyString(), anyString());
    }

    @Test
    void enregistrerAcompte_whenSuffisant_confirmsReservationAndFeedsCaisse() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(5))
                .dateDepart(LocalDateTime.now().plusDays(7))
                .montantPrevu(BigDecimal.valueOf(30000))
                .acompteRequis(BigDecimal.valueOf(9000))
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new AcompteRequest();
        request.setMontant(BigDecimal.valueOf(9000));
        request.setMode("ESPECES");

        var response = reservationService.enregistrerAcompte(1L, request);

        assertThat(response.getStatut()).isEqualTo("CONFIRMEE");
        assertThat(response.getAcompte()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(response.isAcompteSuffisant()).isTrue();
        verify(caisseService).enregistrer(any(), eq(SensMouvement.ENTREE),
                eq(TypeMouvementCaisse.ACOMPTE_RESERVATION), eq(BigDecimal.valueOf(9000)),
                any(), anyString(), any(), eq("RESERVATION"), eq(1L), any());
    }

    @Test
    void enregistrerAcompte_whenPartiel_leavesReservationEnAttente() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(5))
                .dateDepart(LocalDateTime.now().plusDays(7))
                .montantPrevu(BigDecimal.valueOf(30000))
                .acompteRequis(BigDecimal.valueOf(9000))
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new AcompteRequest();
        request.setMontant(BigDecimal.valueOf(4000));
        request.setMode("ESPECES");

        var response = reservationService.enregistrerAcompte(1L, request);

        assertThat(response.getStatut()).isEqualTo("EN_ATTENTE");
        assertThat(response.getResteAcompte()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void updateStatut_toConfirmee_withoutAcompte_isRejected() {
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(1))
                .dateDepart(LocalDateTime.now().plusDays(2))
                .montantPrevu(BigDecimal.valueOf(30000))
                .acompteRequis(BigDecimal.valueOf(9000))
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateStatut(1L, "CONFIRMEE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("acompte");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void politiqueAnnulation_dansLesDelais_remboursementIntegral() {
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(5))
                .dateDepart(LocalDateTime.now().plusDays(7))
                .dateLimiteAnnulation(LocalDateTime.now().plusDays(3))
                .acompte(BigDecimal.valueOf(9000))
                .statut(StatutReservation.CONFIRMEE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        var politique = reservationService.politiqueAnnulation(1L);

        assertThat(politique.isDansLesDelais()).isTrue();
        assertThat(politique.getPenalite()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(politique.getRemboursement()).isEqualByComparingTo(BigDecimal.valueOf(9000));
    }

    @Test
    void politiqueAnnulation_horsDelais_acompteConserve() {
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                // Arrivée dans 12 h : le préavis de 48 h est franchi.
                .dateArrivee(LocalDateTime.now().plusHours(12))
                .dateDepart(LocalDateTime.now().plusDays(2))
                .dateLimiteAnnulation(LocalDateTime.now().minusHours(36))
                .acompte(BigDecimal.valueOf(9000))
                .statut(StatutReservation.CONFIRMEE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        var politique = reservationService.politiqueAnnulation(1L);

        assertThat(politique.isDansLesDelais()).isFalse();
        assertThat(politique.getPenalite()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(politique.getRemboursement()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void annuler_horsDelais_retientAcompteEtNeRembourssePas() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusHours(12))
                .dateDepart(LocalDateTime.now().plusDays(2))
                .dateLimiteAnnulation(LocalDateTime.now().minusHours(36))
                .acompte(BigDecimal.valueOf(9000))
                .statut(StatutReservation.CONFIRMEE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.annuler(1L, null);

        assertThat(response.getStatut()).isEqualTo("ANNULEE");
        assertThat(response.getPenaliteAnnulation()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(response.getMontantRembourse()).isEqualByComparingTo(BigDecimal.ZERO);
        // Aucune sortie de caisse : l'acompte reste acquis à la résidence.
        verifyNoInteractions(caisseService);
    }

    @Test
    void annuler_dansLesDelais_avecRemboursement_sortDeCaisse() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(5))
                .dateDepart(LocalDateTime.now().plusDays(7))
                .dateLimiteAnnulation(LocalDateTime.now().plusDays(3))
                .acompte(BigDecimal.valueOf(9000))
                .statut(StatutReservation.CONFIRMEE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new AnnulationRequest();
        request.setMotif("Empêchement du client");
        request.setRembourser(true);

        var response = reservationService.annuler(1L, request);

        assertThat(response.getPenaliteAnnulation()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getMontantRembourse()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        verify(caisseService).enregistrer(any(), eq(SensMouvement.SORTIE),
                eq(TypeMouvementCaisse.REMBOURSEMENT), eq(BigDecimal.valueOf(9000)),
                any(), anyString(), any(), eq("RESERVATION_ANNULEE"), eq(1L), any());
    }

    /**
     * Sans acompte exigé (acompteRequis à zéro), la confirmation directe reste
     * possible : la règle porte sur le fait de couvrir le minimum demandé, pas
     * sur l'existence d'un versement.
     */
    @Test
    void updateStatut_toConfirmee_whenAucunAcompteExige_triggersArriveePrevueNotification() {
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

    /**
     * Un passage à ANNULEE via le changement de statut est redirigé vers la
     * procédure d'annulation, qui applique la règle des 48 h et notifie —
     * on ne peut plus annuler « en silence » par un simple changement d'état.
     */
    @Test
    void updateStatut_toAnnulee_appliesCancellationPolicy() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Reservation reservation = Reservation.builder()
                .id(1L).numeroReservation("RES-1")
                .client(Client.builder().id(1L).civilite("M.").nom("KOUASSI").build())
                .dateArrivee(LocalDateTime.now().plusDays(5))
                .dateDepart(LocalDateTime.now().plusDays(7))
                .dateLimiteAnnulation(LocalDateTime.now().plusDays(3))
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.updateStatut(1L, "ANNULEE");

        assertThat(response.getStatut()).isEqualTo("ANNULEE");
        assertThat(response.getDateAnnulation()).isNotNull();
        verify(notificationService).creer(eq("RESERVATION_ANNULEE"), anyString(), anyString(), anyString());
        verify(notificationService, never()).creer(eq("ARRIVEE_PREVUE"), anyString(), anyString(), anyString());
    }
}
