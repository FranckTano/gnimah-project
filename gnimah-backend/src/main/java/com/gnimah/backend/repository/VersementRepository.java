package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Versement;
import com.gnimah.backend.entity.enums.StatutVersement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VersementRepository extends JpaRepository<Versement, Long> {

    Page<Versement> findByStatutOrderByDateVersementDesc(StatutVersement statut, Pageable pageable);

    List<Versement> findByDateVersementBetweenOrderByDateVersementDesc(LocalDate debut, LocalDate fin);

    List<Versement> findByCaisseIdOrderByDateVersementDesc(Long caisseId);

    @Query("SELECT COALESCE(SUM(v.montant), 0) FROM Versement v " +
           "WHERE v.statut <> com.gnimah.backend.entity.enums.StatutVersement.ANNULE " +
           "AND v.dateVersement BETWEEN :debut AND :fin")
    BigDecimal sumPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(v) FROM Versement v WHERE v.statut = :statut")
    long countByStatutValue(@Param("statut") StatutVersement statut);

    @Query("SELECT MAX(CAST(SUBSTRING(v.numero, LENGTH(v.numero) - 3, 4) AS int)) FROM Versement v " +
           "WHERE v.numero LIKE CONCAT(:prefix, '%')")
    Integer findMaxSequence(@Param("prefix") String prefix);

    /**
     * Recouvrement de période sur la même caisse : deux versements ne doivent
     * pas revendiquer la même recette. Les versements annulés sont ignorés.
     */
    @Query("SELECT v FROM Versement v WHERE v.caisse.id = :caisseId " +
           "AND v.statut <> com.gnimah.backend.entity.enums.StatutVersement.ANNULE " +
           "AND v.periodeDebut <= :fin AND v.periodeFin >= :debut")
    List<Versement> findChevauchements(@Param("caisseId") Long caisseId,
                                       @Param("debut") LocalDate debut,
                                       @Param("fin") LocalDate fin);
}
