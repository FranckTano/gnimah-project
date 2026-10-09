package com.gnimah.backend.service;

import com.gnimah.backend.dto.sejour.SejourRequest;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.Client;
import com.gnimah.backend.entity.Sejour;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.TypeChambre;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.PaiementRepository;
import com.gnimah.backend.repository.ReservationRepository;
import com.gnimah.backend.entity.Paiement;
import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.StatutReservation;
import org.mockito.ArgumentCaptor;
import java.util.List;
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
import java.time.LocalDate;
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
    // Ajouté lors de l'enregistrement de l'acompte au check-in : sans ce mock,
    // checkIn() lève un NullPointerException dès qu'un montant est payé.
    @Mock private PaiementRepository paiementRepository;
    // Tout encaissement alimente désormais une caisse : le check-in avec acompte
    // passe par ce service, qui doit donc être simulé.
    @Mock private CaisseService caisseService;
    // Arrivée d'une réservation : reprise de l'acompte et contrôle des chevauchements.
    @Mock private ReservationRepository reservationRepository;

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
        request.setDateEntree(LocalDate.now());
        request.setDateSortie(LocalDate.now().plusDays(2));

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
        LocalDate entree = LocalDate.now();
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
        assertThat(response.getMontantBrut()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(response.getMontantTotal()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        assertThat(response.getResteAPayer()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(response.getRemiseMontant()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(chambre.getEtat()).isEqualTo(EtatChambre.OCCUPEE);
        assertThat(client.getNbSejours()).isEqualTo(3);
        // L'acompte doit avoir alimenté une caisse, sinon la recette serait
        // invisible en trésorerie et ne pourrait jamais être versée en banque.
        verify(caisseService).enregistrer(any(), eq(SensMouvement.ENTREE),
                eq(TypeMouvementCaisse.ENCAISSEMENT_SEJOUR), eq(BigDecimal.valueOf(20000)),
                any(), anyString(), anyString(), eq("SEJOUR"), any(), any());
    }

    @Test
    void checkIn_withRemisePourcentage_deductsFromMontantBrut() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        SejourRequest request = new SejourRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setTypeLocation("SEJOUR");
        LocalDate entree = LocalDate.now();
        request.setDateEntree(entree);
        request.setDateSortie(entree.plusDays(2));
        request.setRemiseType("POURCENTAGE");
        request.setRemiseValeur(BigDecimal.valueOf(10));
        request.setRemiseMotif("Client fidèle");

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = sejourService.checkIn(request);

        // 2 nuits * 15000 = 30000, remise de 10 % = 3000
        assertThat(response.getMontantBrut()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(response.getRemiseMontant()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(response.getMontantTotal()).isEqualByComparingTo(BigDecimal.valueOf(27000));
        assertThat(response.getRemiseMotif()).isEqualTo("Client fidèle");
    }

    @Test
    void checkIn_withTarifPersonnalise_overridesTarifCatalogue() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        SejourRequest request = new SejourRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setTypeLocation("SEJOUR");
        LocalDate entree = LocalDate.now();
        request.setDateEntree(entree);
        request.setDateSortie(entree.plusDays(2));
        request.setTarifPersonnalise(BigDecimal.valueOf(12000));

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = sejourService.checkIn(request);

        assertThat(response.getTarifApplique()).isEqualByComparingTo(BigDecimal.valueOf(12000));
        assertThat(response.getMontantTotal()).isEqualByComparingTo(BigDecimal.valueOf(24000));
    }

    @Test
    void checkIn_whenRemiseExceedsMontant_throwsBusinessException() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        SejourRequest request = new SejourRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setTypeLocation("SEJOUR");
        LocalDate entree = LocalDate.now();
        request.setDateEntree(entree);
        request.setDateSortie(entree.plusDays(1));
        request.setRemiseType("MONTANT");
        request.setRemiseValeur(BigDecimal.valueOf(99000));

        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);

        assertThatThrownBy(() -> sejourService.checkIn(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépasse");

        verify(sejourRepository, never()).save(any());
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
        // Séjour soldé : aucune alerte d'impayé ne doit être levée.
        verify(notificationService, never()).creer(eq("IMPAYE"), anyString(), anyString(), anyString());
    }

    @Test
    void checkOut_withSoldeImpaye_raisesImpayeNotification() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).build();
        Sejour sejour = Sejour.builder().id(1L).statut("EN_COURS").numeroRecu("REC-1")
                .typeLocation(com.gnimah.backend.entity.enums.TypeLocation.SEJOUR)
                .chambre(chambre)
                .client(Client.builder().id(1L).civilite("M.").nom("DIALLO").build())
                .montantTotal(BigDecimal.valueOf(45000))
                .montantPaye(BigDecimal.valueOf(20000))
                .resteAPayer(BigDecimal.valueOf(25000))
                .build();
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejour));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        sejourService.checkOut(1L);

        // Un départ avec solde ne bloque pas la sortie, mais ne doit pas passer inaperçu.
        verify(notificationService).creer(eq("IMPAYE"), anyString(), anyString(), anyString());
    }

    @Test
    void appliquerRemise_whenNouveauTotalBelowMontantPaye_throwsBusinessException() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Sejour sejour = Sejour.builder().id(1L).statut("EN_COURS").numeroRecu("REC-1")
                .typeLocation(com.gnimah.backend.entity.enums.TypeLocation.SEJOUR)
                .chambre(chambre).nbJours(2)
                .client(Client.builder().id(1L).civilite("M.").nom("DIALLO").build())
                .tarifApplique(BigDecimal.valueOf(15000))
                .montantBrut(BigDecimal.valueOf(30000))
                .montantTotal(BigDecimal.valueOf(30000))
                .montantPaye(BigDecimal.valueOf(30000))
                .build();
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejour));

        var request = new com.gnimah.backend.dto.sejour.RemiseSejourRequest();
        request.setRemiseType("POURCENTAGE");
        request.setRemiseValeur(BigDecimal.valueOf(50));

        assertThatThrownBy(() -> sejourService.appliquerRemise(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("remboursement");
    }

    // ------------------------------------------------------------------
    // Arrivée d'un client qui avait réservé
    // ------------------------------------------------------------------

    private SejourRequest requeteArrivee(Long reservationId, BigDecimal versementArrivee) {
        SejourRequest request = new SejourRequest();
        request.setClientId(1L);
        request.setChambreId(10L);
        request.setTypeLocation("SEJOUR");
        LocalDate entree = LocalDate.now();
        request.setDateEntree(entree);
        request.setDateSortie(entree.plusDays(2));
        request.setReservationId(reservationId);
        request.setMontantPaye(versementArrivee);
        return request;
    }

    @Test
    void checkIn_withReservation_creditsAcompteWithoutSecondCashEntry() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Reservation reservation = Reservation.builder().id(7L).numeroReservation("RES-7").client(client)
                .statut(StatutReservation.CONFIRMEE).acompte(BigDecimal.valueOf(9000))
                .modeAcompte(ModePaiement.WAVE).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = sejourService.checkIn(requeteArrivee(7L, BigDecimal.ZERO));

        // 2 nuits × 15 000 = 30 000, dont 9 000 déjà versés à la réservation.
        assertThat(response.getMontantPaye()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(response.getResteAPayer()).isEqualByComparingTo(BigDecimal.valueOf(21000));
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ARRIVEE);

        ArgumentCaptor<Paiement> paiement = ArgumentCaptor.forClass(Paiement.class);
        verify(paiementRepository).save(paiement.capture());
        assertThat(paiement.getValue().getMontant()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(paiement.getValue().getMode()).isEqualTo(ModePaiement.WAVE);
        // L'acompte est en caisse depuis la réservation : pas de seconde écriture.
        verifyNoInteractions(caisseService);
    }

    @Test
    void checkIn_withReservationAndVersementArrivee_recordsOnlyTheNewCash() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Reservation reservation = Reservation.builder().id(7L).numeroReservation("RES-7").client(client)
                .statut(StatutReservation.CONFIRMEE).acompte(BigDecimal.valueOf(9000)).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = sejourService.checkIn(requeteArrivee(7L, BigDecimal.valueOf(21000)));

        assertThat(response.getMontantPaye()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(response.getResteAPayer()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(paiementRepository, times(2)).save(any(Paiement.class));
        verify(caisseService).enregistrer(any(), eq(SensMouvement.ENTREE),
                eq(TypeMouvementCaisse.ENCAISSEMENT_SEJOUR), eq(BigDecimal.valueOf(21000)),
                any(), anyString(), anyString(), eq("SEJOUR"), any(), any());
    }

    @Test
    void checkIn_withCancelledReservation_isRefused() {
        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Reservation reservation = Reservation.builder().id(7L).numeroReservation("RES-7").client(client)
                .statut(StatutReservation.ANNULEE).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> sejourService.checkIn(requeteArrivee(7L, BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("annulée");
        verify(sejourRepository, never()).save(any());
    }

    @Test
    void checkIn_withReservationOfAnotherClient_isRefused() {
        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Client autre = Client.builder().id(2L).civilite("Mme").nom("KOUASSI").build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Reservation reservation = Reservation.builder().id(7L).numeroReservation("RES-7").client(autre)
                .statut(StatutReservation.CONFIRMEE).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> sejourService.checkIn(requeteArrivee(7L, BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("KOUASSI");
    }

    // ------------------------------------------------------------------
    // Prolongation
    // ------------------------------------------------------------------

    private Sejour sejourDeuxNuits(Chambre chambre) {
        LocalDateTime arrivee = LocalDate.now().atStartOfDay();
        return Sejour.builder().id(1L).statut("EN_COURS").numeroRecu("REC-1")
                .typeLocation(com.gnimah.backend.entity.enums.TypeLocation.SEJOUR)
                .chambre(chambre).nbJours(2)
                .dateEntree(arrivee).dateSortie(arrivee.plusDays(2))
                .client(Client.builder().id(1L).civilite("M.").nom("DIALLO").build())
                .tarifApplique(BigDecimal.valueOf(15000))
                .montantBrut(BigDecimal.valueOf(30000))
                .montantTotal(BigDecimal.valueOf(30000))
                .montantPaye(BigDecimal.valueOf(10000))
                .resteAPayer(BigDecimal.valueOf(20000))
                .build();
    }

    @Test
    void appliquerRemise_withNewDateSortie_extendsStayAndRecomputesTotal() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Sejour sejour = sejourDeuxNuits(chambre);
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejour));
        when(reservationRepository.findConflits(eq(10L), any(), any())).thenReturn(List.of());
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.gnimah.backend.dto.sejour.RemiseSejourRequest();
        request.setDateSortie(LocalDate.now().plusDays(4));

        var response = sejourService.appliquerRemise(1L, request);

        assertThat(response.getNbJours()).isEqualTo(4);
        assertThat(response.getMontantTotal()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        assertThat(response.getResteAPayer()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.getDateSortie()).isEqualTo(LocalDate.now().plusDays(4).atStartOfDay());
    }

    @Test
    void appliquerRemise_whenExtensionHitsAnotherReservation_isRefused() {
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Sejour sejour = sejourDeuxNuits(chambre);
        Reservation suivante = Reservation.builder().id(9L).numeroReservation("RES-9")
                .client(Client.builder().id(3L).civilite("M.").nom("TRAORE").build())
                .dateArrivee(LocalDate.now().plusDays(3).atStartOfDay())
                .statut(StatutReservation.CONFIRMEE).build();
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejour));
        when(reservationRepository.findConflits(eq(10L), any(), any())).thenReturn(List.of(suivante));

        var request = new com.gnimah.backend.dto.sejour.RemiseSejourRequest();
        request.setDateSortie(LocalDate.now().plusDays(5));

        assertThatThrownBy(() -> sejourService.appliquerRemise(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TRAORE");
        verify(sejourRepository, never()).save(any());
    }

    @Test
    void appliquerRemise_whenDateSortieNotAfterArrivee_isRefused() {
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.OCCUPEE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        when(sejourRepository.findById(1L)).thenReturn(Optional.of(sejourDeuxNuits(chambre)));

        var request = new com.gnimah.backend.dto.sejour.RemiseSejourRequest();
        request.setDateSortie(LocalDate.now());

        assertThatThrownBy(() -> sejourService.appliquerRemise(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("postérieure");
    }

    // ------------------------------------------------------------------
    // Pas de surréservation d'une chambre promise à un autre client
    // ------------------------------------------------------------------

    @Test
    void checkIn_surUneChambreReserveeEtConfirmeeParUnAutreClient_estRefuse() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Reservation autre = Reservation.builder().id(9L).numeroReservation("RES-9")
                .client(Client.builder().id(3L).civilite("M.").nom("TRAORE").build())
                .dateArrivee(LocalDate.now().plusDays(1).atStartOfDay())
                .statut(StatutReservation.CONFIRMEE).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(reservationRepository.findConflits(eq(10L), any(), any())).thenReturn(List.of(autre));

        assertThatThrownBy(() -> sejourService.checkIn(requeteArrivee(null, BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TRAORE");
        verify(sejourRepository, never()).save(any());
        assertThat(chambre.getEtat()).isEqualTo(EtatChambre.LIBRE);
    }

    @Test
    void checkIn_uneReservationSansAcompte_neTientPasLaChambre() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("agent", null));
        Client client = Client.builder().id(1L).civilite("M.").nom("DIALLO").nbSejours(0).build();
        Chambre chambre = Chambre.builder().id(10L).numero("101").etat(EtatChambre.LIBRE)
                .type(TypeChambre.STANDARD).tarifNuitee(BigDecimal.valueOf(15000)).build();
        Reservation enAttente = Reservation.builder().id(9L).numeroReservation("RES-9")
                .client(Client.builder().id(3L).civilite("M.").nom("TRAORE").build())
                .dateArrivee(LocalDate.now().plusDays(1).atStartOfDay())
                .statut(StatutReservation.EN_ATTENTE).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(chambreService.findById(10L)).thenReturn(chambre);
        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.empty());
        when(sejourRepository.findMaxSequence(any())).thenReturn(0);
        when(reservationRepository.findConflits(eq(10L), any(), any())).thenReturn(List.of(enAttente));
        when(sejourRepository.save(any(Sejour.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = sejourService.checkIn(requeteArrivee(null, BigDecimal.ZERO));

        assertThat(response.getMontantTotal()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(chambre.getEtat()).isEqualTo(EtatChambre.OCCUPEE);
    }
}
