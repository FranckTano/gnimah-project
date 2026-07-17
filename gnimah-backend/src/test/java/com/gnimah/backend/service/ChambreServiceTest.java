package com.gnimah.backend.service;

import com.gnimah.backend.dto.chambre.ChambreRequest;
import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.ChambreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChambreServiceTest {

    @Mock private ChambreRepository chambreRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ChambreService chambreService;

    private ChambreRequest validRequest() {
        ChambreRequest request = new ChambreRequest();
        request.setNumero("501");
        request.setType("STANDARD");
        request.setCapacite(2);
        request.setTarifPassage(BigDecimal.valueOf(5000));
        request.setTarifNuitee(BigDecimal.valueOf(15000));
        return request;
    }

    @Test
    void create_withExistingNumero_throwsBusinessException() {
        ChambreRequest request = validRequest();
        when(chambreRepository.findByNumero("501")).thenReturn(Optional.of(new Chambre()));

        assertThatThrownBy(() -> chambreService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("501");

        verify(chambreRepository, never()).save(any());
    }

    @Test
    void create_withNewNumero_savesChambre() {
        ChambreRequest request = validRequest();
        when(chambreRepository.findByNumero("501")).thenReturn(Optional.empty());
        when(chambreRepository.save(any(Chambre.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = chambreService.create(request);

        assertThat(response.getNumero()).isEqualTo("501");
        assertThat(response.getEtat()).isEqualTo("LIBRE");
    }

    @Test
    void updateEtat_toMaintenance_triggersMaintenanceNotification() {
        Chambre chambre = Chambre.builder().id(1L).numero("101").etat(EtatChambre.LIBRE)
                .type(com.gnimah.backend.entity.enums.TypeChambre.STANDARD)
                .tarifPassage(BigDecimal.TEN).tarifNuitee(BigDecimal.TEN).build();
        when(chambreRepository.findById(1L)).thenReturn(Optional.of(chambre));
        when(chambreRepository.save(any(Chambre.class))).thenAnswer(inv -> inv.getArgument(0));

        chambreService.updateEtat(1L, "EN_MAINTENANCE");

        verify(notificationService).creer(eq("MAINTENANCE"), anyString(), anyString(), anyString());
    }

    @Test
    void updateEtat_toLibre_doesNotTriggerNotification() {
        Chambre chambre = Chambre.builder().id(1L).numero("101").etat(EtatChambre.A_NETTOYER)
                .type(com.gnimah.backend.entity.enums.TypeChambre.STANDARD)
                .tarifPassage(BigDecimal.TEN).tarifNuitee(BigDecimal.TEN).build();
        when(chambreRepository.findById(1L)).thenReturn(Optional.of(chambre));
        when(chambreRepository.save(any(Chambre.class))).thenAnswer(inv -> inv.getArgument(0));

        chambreService.updateEtat(1L, "LIBRE");

        verifyNoInteractions(notificationService);
    }
}
