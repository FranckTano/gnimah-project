package com.gnimah.backend.service;

import com.gnimah.backend.entity.AuditLog;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Utilisateur utilisateur, String action, String entite, Long entiteId, String details) {
        AuditLog log = AuditLog.builder()
                .utilisateur(utilisateur)
                .username(utilisateur != null ? utilisateur.getUsername() : "system")
                .action(action)
                .entite(entite)
                .entiteId(entiteId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
