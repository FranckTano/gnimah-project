package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    @Query("SELECT e FROM Evenement e WHERE e.dateDebut BETWEEN :debut AND :fin ORDER BY e.dateDebut")
    List<Evenement> findByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    List<Evenement> findByClientId(Long clientId);
}
