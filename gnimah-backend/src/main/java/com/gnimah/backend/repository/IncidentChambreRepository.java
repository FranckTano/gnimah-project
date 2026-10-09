package com.gnimah.backend.repository;

import com.gnimah.backend.entity.IncidentChambre;
import com.gnimah.backend.entity.enums.StatutIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentChambreRepository extends JpaRepository<IncidentChambre, Long> {

    List<IncidentChambre> findAllByOrderByDateIncidentDesc();

    Page<IncidentChambre> findAllByOrderByDateIncidentDesc(Pageable pageable);

    List<IncidentChambre> findByStatutOrderByDateIncidentDesc(StatutIncident statut);

    List<IncidentChambre> findByChambreIdOrderByDateIncidentDesc(Long chambreId);

    List<IncidentChambre> findBySejourIdOrderByDateIncidentDesc(Long sejourId);

    /** Incidents encore à traiter, urgents d'abord. */
    @Query("SELECT i FROM IncidentChambre i WHERE i.statut IN " +
           "(com.gnimah.backend.entity.enums.StatutIncident.DECLARE, " +
           " com.gnimah.backend.entity.enums.StatutIncident.EN_COURS) " +
           "ORDER BY i.gravite DESC, i.dateIncident ASC")
    List<IncidentChambre> findOuverts();

    @Query("SELECT COUNT(i) FROM IncidentChambre i WHERE i.statut IN " +
           "(com.gnimah.backend.entity.enums.StatutIncident.DECLARE, " +
           " com.gnimah.backend.entity.enums.StatutIncident.EN_COURS)")
    long countOuverts();

    @Query("SELECT COALESCE(SUM(i.coutReel), 0) FROM IncidentChambre i " +
           "WHERE i.dateIncident >= :debut AND i.dateIncident < :fin")
    BigDecimal sumCoutPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT i.type, COUNT(i), COALESCE(SUM(i.coutReel), 0) FROM IncidentChambre i " +
           "WHERE i.dateIncident >= :debut AND i.dateIncident < :fin GROUP BY i.type")
    List<Object[]> repartitionParType(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MAX(CAST(SUBSTRING(i.numero, LENGTH(i.numero) - 3, 4) AS int)) FROM IncidentChambre i " +
           "WHERE i.numero LIKE CONCAT(:prefix, '%')")
    Integer findMaxSequence(@Param("prefix") String prefix);
}
