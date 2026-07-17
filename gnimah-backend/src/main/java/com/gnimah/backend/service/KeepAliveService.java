package com.gnimah.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-ping toutes les 13 min (< seuil de mise en veille Render de 15 min).
 * Tourne dans la JVM déjà active, donc pas soumis aux aléas d'un planificateur
 * externe (contrairement à un cron GitHub Actions) : tant que le service est
 * éveillé, il se maintient éveillé lui-même. app.keepalive.target-url vide
 * (par défaut en local/test) désactive le ping.
 */
@Service
public class KeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    @Value("${app.keepalive.target-url:}")
    private String targetUrl;

    @Scheduled(fixedDelay = 780_000, initialDelay = 60_000)
    public void ping() {
        if (targetUrl == null || targetUrl.isBlank()) {
            return;
        }
        try {
            REST_TEMPLATE.getForObject(targetUrl, String.class);
            log.info("[KeepAlive] Ping OK -> {}", targetUrl);
        } catch (Exception e) {
            log.warn("[KeepAlive] Ping echoue -> {} : {}", targetUrl, e.getMessage());
        }
    }
}
