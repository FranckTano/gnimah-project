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
}
