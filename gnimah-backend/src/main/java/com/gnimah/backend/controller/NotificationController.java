package com.gnimah.backend.controller;

import com.gnimah.backend.dto.notification.NotificationResponse;
import com.gnimah.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lister les notifications récentes")
    public ResponseEntity<List<NotificationResponse>> findRecentes() {
        return ResponseEntity.ok(notificationService.findRecentes());
    }

    @GetMapping("/non-lues/count")
    @Operation(summary = "Compter les notifications non lues")
    public ResponseEntity<Map<String, Long>> countNonLues() {
        return ResponseEntity.ok(Map.of("count", notificationService.countNonLues()));
    }

    @PatchMapping("/{id}/lu")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<Void> marquerLue(@PathVariable Long id) {
        notificationService.marquerLue(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/lu-toutes")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<Void> marquerToutesLues() {
        notificationService.marquerToutesLues();
        return ResponseEntity.ok().build();
    }
}
