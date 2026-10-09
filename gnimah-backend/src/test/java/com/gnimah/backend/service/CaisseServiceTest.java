package com.gnimah.backend.service;

import com.gnimah.backend.dto.caisse.FermetureCaisseRequest;
import com.gnimah.backend.dto.caisse.OuvertureCaisseRequest;
import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.MouvementCaisse;
import com.gnimah.backend.entity.SessionCaisse;
import com.gnimah.backend.entity.enums.ModePaiement;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.StatutSessionCaisse;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.CaisseRepository;
import com.gnimah.backend.repository.MouvementCaisseRepository;
import com.gnimah.backend.repository.SessionCaisseRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Le solde d'une caisse doit toujours correspondre aux espèces du tiroir :
 * c'est sur lui que reposent le comptage de fermeture et le plafond des
 * versements.
 */
@ExtendWith(MockitoExtension.class)
class CaisseServiceTest {

    @Mock private CaisseRepository caisseRepository;
    @Mock private SessionCaisseRepository sessionRepository;
    @Mock private MouvementCaisseRepository mouvementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private CaisseService caisseService;

    private Caisse caisse(String solde) {
        return Caisse.builder().id(1L).nom("Caisse Réception").solde(new BigDecimal(solde)).build();
    }

    private OuvertureCaisseRequest ouverture(String fond) {
        OuvertureCaisseRequest request = new OuvertureCaisseRequest();
        request.setCaisseId(1L);
        request.setFondCaisse(new BigDecimal(fond));
        return request;
    }

    private void activiteDeVacation(String entreesEspeces, String sortiesEspeces) {
        lenient().when(mouvementRepository.sumActiviteBySessionAndSens(any(), eq(SensMouvement.ENTREE)))
                .thenReturn(new BigDecimal(entreesEspeces));
        lenient().when(mouvementRepository.sumActiviteBySessionAndSens(any(), eq(SensMouvement.SORTIE)))
                .thenReturn(new BigDecimal(sortiesEspeces));
        lenient().when(mouvementRepository.sumActiviteEspecesBySessionAndSens(any(), eq(SensMouvement.ENTREE)))
                .thenReturn(new BigDecimal(entreesEspeces));
        lenient().when(mouvementRepository.sumActiviteEspecesBySessionAndSens(any(), eq(SensMouvement.SORTIE)))
                .thenReturn(new BigDecimal(sortiesEspeces));
    }

    private void ouvertureAutorisee(Caisse caisse) {
        when(caisseRepository.findById(1L)).thenReturn(Optional.of(caisse));
        when(sessionRepository.findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(1L, StatutSessionCaisse.OUVERTE))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        activiteDeVacation("0", "0");
    }

    @Test
    void ouvrir_quandLeFondEstDejaAuSolde_nEnregistreAucunMouvement() {
        // Cas courant : le fond laissé la veille est toujours dans le tiroir.
        Caisse caisse = caisse("20000");
        ouvertureAutorisee(caisse);

        caisseService.ouvrir(ouverture("20000"));

        verify(mouvementRepository, never()).save(any());
        assertThat(caisse.getSolde()).isEqualByComparingTo("20000");
    }

    @Test
    void ouvrir_quandLeFondDepasseLeSolde_enregistreUnApportPourLaDifference() {
        Caisse caisse = caisse("5000");
        ouvertureAutorisee(caisse);
        when(mouvementRepository.save(any(MouvementCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        caisseService.ouvrir(ouverture("20000"));

        ArgumentCaptor<MouvementCaisse> mvt = ArgumentCaptor.forClass(MouvementCaisse.class);
        verify(mouvementRepository).save(mvt.capture());
        assertThat(mvt.getValue().getSens()).isEqualTo(SensMouvement.ENTREE);
        assertThat(mvt.getValue().getType()).isEqualTo(TypeMouvementCaisse.FOND_DE_CAISSE);
        assertThat(mvt.getValue().getMontant()).isEqualByComparingTo("15000");
        assertThat(caisse.getSolde()).isEqualByComparingTo("20000");
    }

    @Test
    void ouvrir_quandIlManqueDesEspeces_enregistreUnAjustementEnSortie() {
        Caisse caisse = caisse("30000");
        ouvertureAutorisee(caisse);
        when(mouvementRepository.save(any(MouvementCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        caisseService.ouvrir(ouverture("20000"));

        ArgumentCaptor<MouvementCaisse> mvt = ArgumentCaptor.forClass(MouvementCaisse.class);
        verify(mouvementRepository).save(mvt.capture());
        assertThat(mvt.getValue().getSens()).isEqualTo(SensMouvement.SORTIE);
        assertThat(mvt.getValue().getType()).isEqualTo(TypeMouvementCaisse.AJUSTEMENT);
        assertThat(mvt.getValue().getMontant()).isEqualByComparingTo("10000");
        assertThat(caisse.getSolde()).isEqualByComparingTo("20000");
    }

    @Test
    void fermer_calculeLeTheoriqueEtRegulariseLeSoldeAuMontantCompte() {
        Caisse caisse = caisse("65000");
        SessionCaisse session = SessionCaisse.builder().id(4L).caisse(caisse)
                .fondCaisse(new BigDecimal("20000")).statut(StatutSessionCaisse.OUVERTE).build();
        when(sessionRepository.findById(4L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementRepository.save(any(MouvementCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        activiteDeVacation("50000", "5000");

        FermetureCaisseRequest request = new FermetureCaisseRequest();
        request.setMontantCompte(new BigDecimal("60000"));
        var response = caisseService.fermer(4L, request);

        // 20 000 de fond + 50 000 encaissés − 5 000 décaissés = 65 000 attendus.
        assertThat(response.getMontantTheorique()).isEqualByComparingTo("65000");
        assertThat(response.getEcart()).isEqualByComparingTo("-5000");
        assertThat(response.getStatut()).isEqualTo("FERMEE");

        ArgumentCaptor<MouvementCaisse> mvt = ArgumentCaptor.forClass(MouvementCaisse.class);
        verify(mouvementRepository).save(mvt.capture());
        assertThat(mvt.getValue().getType()).isEqualTo(TypeMouvementCaisse.AJUSTEMENT);
        assertThat(mvt.getValue().getSens()).isEqualTo(SensMouvement.SORTIE);
        assertThat(mvt.getValue().getMontant()).isEqualByComparingTo("5000");
        // Après fermeture, le solde est celui du tiroir.
        assertThat(caisse.getSolde()).isEqualByComparingTo("60000");
    }

    @Test
    void fermer_sansEcart_neRegulariseRien() {
        Caisse caisse = caisse("65000");
        SessionCaisse session = SessionCaisse.builder().id(4L).caisse(caisse)
                .fondCaisse(new BigDecimal("20000")).statut(StatutSessionCaisse.OUVERTE).build();
        when(sessionRepository.findById(4L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
        activiteDeVacation("50000", "5000");

        FermetureCaisseRequest request = new FermetureCaisseRequest();
        request.setMontantCompte(new BigDecimal("65000"));
        var response = caisseService.fermer(4L, request);

        assertThat(response.getEcart()).isEqualByComparingTo("0");
        verify(mouvementRepository, never()).save(any());
    }

    @Test
    void enregistrer_unReglementWave_estJournaliseSansDeplacerLeSolde() {
        Caisse caisse = caisse("10000");
        when(sessionRepository.findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(1L, StatutSessionCaisse.OUVERTE))
                .thenReturn(Optional.empty());
        when(mouvementRepository.save(any(MouvementCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        MouvementCaisse mvt = caisseService.enregistrer(caisse, SensMouvement.ENTREE,
                TypeMouvementCaisse.ENCAISSEMENT_SEJOUR, new BigDecimal("30000"), ModePaiement.WAVE,
                "Paiement Wave", null, "PAIEMENT", 1L, null);

        assertThat(mvt.getMode()).isEqualTo(ModePaiement.WAVE);
        assertThat(caisse.getSolde()).isEqualByComparingTo("10000");
        verify(caisseRepository, never()).save(any());
    }

    @Test
    void enregistrer_uneSortieEnEspecesSuperieureAuSolde_estRefusee() {
        Caisse caisse = caisse("10000");
        when(sessionRepository.findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(1L, StatutSessionCaisse.OUVERTE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> caisseService.enregistrer(caisse, SensMouvement.SORTIE,
                TypeMouvementCaisse.DEPENSE, new BigDecimal("15000"), ModePaiement.ESPECES,
                "Achat", null, "DEPENSE", 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insuffisantes");
        assertThat(caisse.getSolde()).isEqualByComparingTo("10000");
    }
}
