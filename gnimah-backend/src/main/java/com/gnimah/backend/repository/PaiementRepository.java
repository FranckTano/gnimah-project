package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findBySejourId(Long sejourId);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.sejour.id = :sejourId")
    BigDecimal sumMontantBySejour(@Param("sejourId") Long sejourId);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    BigDecimal sumEncaissementsPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT p.mode, COUNT(p), COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin GROUP BY p.mode")
    List<Object[]> findRepartitionModePaiement(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
