package com.gnimah.backend.service;

import com.gnimah.backend.dto.incident.ResolutionIncidentRequest;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.IncidentChambre;
import com.gnimah.backend.entity.enums.StatutIncident;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private IncidentChambreRepository incidentRepository;
    @Mock private ChambreRepository chambreRepository;
    @Mock private SejourRepository sejourRepository;
    @Mock private ArticleStockRepository articleStockRepository;
    @Mock private TacheEntretienRepository tacheRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private StockService stockService;
    @Mock private DepenseService depenseService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private IncidentService incidentService;

    private ResolutionIncidentRequest resolution(String statut) {
        ResolutionIncidentRequest request = new ResolutionIncidentRequest();
        request.setStatut(statut);
        request.setCoutReel(new BigDecimal("12000"));
        return request;
    }

    @Test
    void resoudre_unIncidentDejaRepare_estRefuse() {
        // Un double clic sur « Clôturer » ne doit pas créer une seconde dépense.
        IncidentChambre incident = IncidentChambre.builder().id(3L).numero("INC-1")
                .chambre(Chambre.builder().id(10L).numero("101").build())
                .statut(StatutIncident.REPARE).coutEstime(BigDecimal.ZERO).build();
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.resoudre(3L, resolution("REPARE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà résolu");
        verifyNoInteractions(depenseService, sejourRepository);
    }

    @Test
    void resoudre_avecUneIssueNonTerminale_estRefuse() {
        IncidentChambre incident = IncidentChambre.builder().id(3L).numero("INC-1")
                .chambre(Chambre.builder().id(10L).numero("101").build())
                .statut(StatutIncident.DECLARE).coutEstime(BigDecimal.ZERO).build();
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.resoudre(3L, resolution("EN_COURS")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(depenseService);
    }
}
