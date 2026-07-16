package com.gnimah.backend.service;

import com.gnimah.backend.dto.notification.NotificationResponse;
import com.gnimah.backend.entity.Notification;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void creer(String type, String titre, String message, String lien) {
        Notification notification = Notification.builder()
                .type(type)
                .titre(titre)
                .message(message)
                .lien(lien)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findRecentes() {
        return notificationRepository.findTop30ByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countNonLues() {
        return notificationRepository.countByLuFalse();
    }

    @Transactional
    public void marquerLue(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setLu(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void marquerToutesLues() {
        notificationRepository.marquerToutesLues();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .titre(n.getTitre())
                .message(n.getMessage())
                .lien(n.getLien())
                .lu(n.isLu())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
