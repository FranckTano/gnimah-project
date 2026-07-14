package com.gnimah.backend.repository;

import com.gnimah.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUtilisateurId(Long utilisateurId, Pageable pageable);
    Page<AuditLog> findByEntite(String entite, Pageable pageable);
}
