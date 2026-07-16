package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByOrderByCreatedAtDesc();
    long countByLuFalse();

    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.lu = false")
    void marquerToutesLues();
}
