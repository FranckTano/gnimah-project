package com.gnimah.backend.repository;

import com.gnimah.backend.entity.MouvementCaisse;
import com.gnimah.backend.entity.enums.SensMouvement;
import com.gnimah.backend.entity.enums.TypeMouvementCaisse;
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
public interface MouvementCaisseRepository extends JpaRepository<MouvementCaisse, Long> {

    List<MouvementCaisse> findBySessionIdOrderByDateMouvementDesc(Long sessionId);

    Page<MouvementCaisse> findByCaisseIdOrderByDateMouvementDesc(Long caisseId, Pageable pageable);

    @Query("SELECT m FROM MouvementCaisse m WHERE m.dateMouvement BETWEEN :debut AND :fin ORDER BY m.dateMouvement DESC")
    List<MouvementCaisse> findPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.session.id = :sessionId AND m.sens = :sens")
    BigDecimal sumBySessionAndSens(@Param("sessionId") Long sessionId, @Param("sens") SensMouvement sens);

    /**
     * Seul le numéraire est comptable physiquement à la fermeture : un règlement
     * Wave ou par carte entre bien en recette, mais ne met aucun billet dans le
     * tiroir. Le montant théorique se calcule donc sur les espèces seules.
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.session.id = :sessionId AND m.sens = :sens " +
           "AND m.mode = com.gnimah.backend.entity.enums.ModePaiement.ESPECES")
    BigDecimal sumEspecesBySessionAndSens(@Param("sessionId") Long sessionId, @Param("sens") SensMouvement sens);

    long countBySessionId(Long sessionId);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.sens = :sens AND m.dateMouvement BETWEEN :debut AND :fin")
    BigDecimal sumBySensPeriode(@Param("sens") SensMouvement sens,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.caisse.id = :caisseId AND m.sens = :sens AND m.dateMouvement BETWEEN :debut AND :fin")
    BigDecimal sumByCaisseSensPeriode(@Param("caisseId") Long caisseId,
                                      @Param("sens") SensMouvement sens,
                                      @Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin);

    @Query("SELECT m.type, m.sens, COUNT(m), COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.dateMouvement BETWEEN :debut AND :fin GROUP BY m.type, m.sens")
    List<Object[]> repartitionParType(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.sens = :sens AND m.type = :type AND m.dateMouvement BETWEEN :debut AND :fin")
    BigDecimal sumParTypePeriode(@Param("sens") SensMouvement sens,
                                 @Param("type") TypeMouvementCaisse type,
                                 @Param("debut") LocalDateTime debut,
                                 @Param("fin") LocalDateTime fin);

    /**
     * Recette en espèces encaissée sur une caisse pour une période, hors fond de
     * caisse : c'est l'assiette d'un versement.
     *
     * Restreinte au numéraire, car la caissière ne peut déposer que ce qu'elle
     * détient physiquement — un règlement Wave est déjà sur le compte de
     * l'opérateur et n'a pas à repasser par elle.
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.caisse.id = :caisseId AND m.sens = com.gnimah.backend.entity.enums.SensMouvement.ENTREE " +
           "AND m.mode = com.gnimah.backend.entity.enums.ModePaiement.ESPECES " +
           "AND m.type <> com.gnimah.backend.entity.enums.TypeMouvementCaisse.FOND_DE_CAISSE " +
           "AND m.dateMouvement BETWEEN :debut AND :fin")
    BigDecimal sumRecettePeriode(@Param("caisseId") Long caisseId,
                                 @Param("debut") LocalDateTime debut,
                                 @Param("fin") LocalDateTime fin);

    List<MouvementCaisse> findByOrigineTypeAndOrigineId(String origineType, Long origineId);
}
