package com.gnimah.backend.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String titre;
    private String message;
    private String lien;
    private boolean lu;
    private LocalDateTime createdAt;
}
