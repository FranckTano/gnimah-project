package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Sejour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SejourRepository extends JpaRepository<Sejour, Long> {

    Optional<Sejour> findByNumeroRecu(String numeroRecu);
    Page<Sejour> findByClientId(Long clientId, Pageable pageable);
    List<Sejour> findByChambreIdAndStatut(Long chambreId, String statut);
    List<Sejour> findByStatut(String statut);

    @Query("SELECT s FROM Sejour s WHERE s.statut = 'EN_COURS'")
    List<Sejour> findSejoursEnCours();

    @Query("SELECT s FROM Sejour s WHERE s.dateSortie BETWEEN :debut AND :fin")
    List<Sejour> findDepartsJour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT s FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin")
    List<Sejour> findArriveesPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(s.montantTotal), 0) FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin")
    BigDecimal sumChiffreAffaires(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(s.montantPaye), 0) FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin")
    BigDecimal sumEncaissements(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(s) FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin")
    long countSejoursPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(AVG(s.nbJours), 0) FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin AND s.nbJours IS NOT NULL")
    Double avgDureeSejour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(DISTINCT s.client.id) FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin")
    long countClientsUniques(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT s.agent.id, COUNT(s), SUM(s.montantTotal) FROM Sejour s " +
           "WHERE s.dateEntree BETWEEN :debut AND :fin GROUP BY s.agent.id")
    List<Object[]> findPerformanceAgents(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT s.typeLocation, COUNT(s) FROM Sejour s WHERE s.dateEntree BETWEEN :debut AND :fin GROUP BY s.typeLocation")
    List<Object[]> countByTypeLocationPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MAX(CAST(SUBSTRING(s.numeroRecu, LENGTH(s.numeroRecu) - 3, 4) AS int)) FROM Sejour s " +
           "WHERE s.numeroRecu LIKE CONCAT(:prefix, '%')")
    Integer findMaxSequence(@Param("prefix") String prefix);
}
