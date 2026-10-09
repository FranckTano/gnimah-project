package com.gnimah.backend.service;

import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.CompteFinancier;
import com.gnimah.backend.entity.MouvementCaisse;
import com.gnimah.backend.entity.Versement;
import com.gnimah.backend.entity.enums.*;
import com.gnimah.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersementServiceTest {

    @Mock private VersementRepository versementRepository;
    @Mock private ActionVersementRepository actionRepository;
    @Mock private CompteFinancierRepository compteRepository;
    @Mock private MouvementCaisseRepository mouvementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private CaisseService caisseService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private VersementService versementService;

    private Versement versement(StatutVersement statut, ModePaiement mode) {
        Caisse caisse = Caisse.builder().id(1L).nom("Caisse Réception").solde(new BigDecimal("50000")).build();
        CompteFinancier compte = CompteFinancier.builder().id(2L).libelle("Wave — recette journalière")
                .type(TypeCompteFinancier.MOBILE_MONEY).numeroTelephone("0700000000")
                .solde(BigDecimal.ZERO).build();
        return Versement.builder().id(5L).numero("VER-202610-0001").caisse(caisse).compte(compte)
                .montant(new BigDecimal("40000")).mode(mode).statut(statut)
                .periodeDebut(LocalDate.now()).periodeFin(LocalDate.now()).dateVersement(LocalDate.now())
                .build();
    }

    @Test
    void deposer_viaWave_sortTouteLaSommeDuTiroirEnEspeces() {
        // La caissière dépose les billets de la recette chez un agent Wave :
        // ils quittent bien le tiroir, le solde de la caisse doit baisser.
        Versement v = versement(StatutVersement.PREPARE, ModePaiement.WAVE);
        when(versementRepository.findById(5L)).thenReturn(Optional.of(v));
        when(versementRepository.save(any(Versement.class))).thenAnswer(inv -> inv.getArgument(0));

        versementService.deposer(5L, null);

        verify(caisseService).enregistrer(eq(v.getCaisse()), eq(SensMouvement.SORTIE),
                eq(TypeMouvementCaisse.VERSEMENT_BANQUE), eq(new BigDecimal("40000")), eq(ModePaiement.ESPECES),
                anyString(), any(), eq("VERSEMENT"), eq(5L), any());
        assertThat(v.getStatut()).isEqualTo(StatutVersement.EFFECTUE);
        assertThat(v.getMode()).isEqualTo(ModePaiement.WAVE);
    }

    @Test
    void annuler_unVersementDepose_remetAuTiroirSelonLeModeDeLaSortie() {
        Versement v = versement(StatutVersement.EFFECTUE, ModePaiement.WAVE);
        MouvementCaisse sortie = MouvementCaisse.builder().sens(SensMouvement.SORTIE).mode(ModePaiement.ESPECES)
                .montant(new BigDecimal("40000")).build();
        when(versementRepository.findById(5L)).thenReturn(Optional.of(v));
        when(mouvementRepository.findByOrigineTypeAndOrigineId("VERSEMENT", 5L)).thenReturn(List.of(sortie));
        when(versementRepository.save(any(Versement.class))).thenAnswer(inv -> inv.getArgument(0));

        versementService.annuler(5L, null);

        verify(caisseService).enregistrer(eq(v.getCaisse()), eq(SensMouvement.ENTREE),
                eq(TypeMouvementCaisse.AJUSTEMENT), eq(new BigDecimal("40000")), eq(ModePaiement.ESPECES),
                anyString(), any(), eq("VERSEMENT_ANNULE"), eq(5L), any());
        assertThat(v.getStatut()).isEqualTo(StatutVersement.ANNULE);
    }

    @Test
    void annuler_unVersementSeulementPrepare_neTouchePasALaCaisse() {
        Versement v = versement(StatutVersement.PREPARE, ModePaiement.ESPECES);
        when(versementRepository.findById(5L)).thenReturn(Optional.of(v));
        when(versementRepository.save(any(Versement.class))).thenAnswer(inv -> inv.getArgument(0));

        versementService.annuler(5L, null);

        verifyNoInteractions(caisseService);
        assertThat(v.getStatut()).isEqualTo(StatutVersement.ANNULE);
    }
}
