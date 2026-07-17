package com.gnimah.backend.service;

import com.gnimah.backend.dto.notification.NotificationResponse;
import com.gnimah.backend.entity.Notification;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void creer_buildsAndSavesUnreadNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.creer("NOUVELLE_RESERVATION", "Nouvelle réservation", "Détail", "/reservations");

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("NOUVELLE_RESERVATION");
        assertThat(saved.isLu()).isFalse();
    }

    @Test
    void marquerLue_unknownId_throwsResourceNotFound() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.marquerLue(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marquerLue_existingNotification_setsLuTrue() {
        Notification notification = Notification.builder().id(1L).type("X").titre("T").lu(false).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.marquerLue(1L);

        assertThat(notification.isLu()).isTrue();
    }

    @Test
    void countNonLues_delegatesToRepository() {
        when(notificationRepository.countByLuFalse()).thenReturn(7L);

        assertThat(notificationService.countNonLues()).isEqualTo(7L);
    }

    @Test
    void findRecentes_mapsEntitiesToResponses() {
        Notification n = Notification.builder().id(1L).type("MAINTENANCE").titre("Titre").message("Msg").lu(false).build();
        when(notificationRepository.findTop30ByOrderByCreatedAtDesc()).thenReturn(List.of(n));

        List<NotificationResponse> result = notificationService.findRecentes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitre()).isEqualTo("Titre");
    }
}
