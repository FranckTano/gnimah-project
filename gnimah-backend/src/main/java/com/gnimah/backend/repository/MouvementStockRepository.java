package com.gnimah.backend.repository;

import com.gnimah.backend.entity.MouvementStock;
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
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByArticleIdOrderByDateMouvementDesc(Long articleId);

    Page<MouvementStock> findAllByOrderByDateMouvementDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementStock m " +
           "WHERE m.sens = com.gnimah.backend.entity.enums.SensMouvement.SORTIE " +
           "AND m.dateMouvement BETWEEN :debut AND :fin")
    BigDecimal sumConsommationPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementStock m " +
           "WHERE m.sens = com.gnimah.backend.entity.enums.SensMouvement.ENTREE " +
           "AND m.dateMouvement BETWEEN :debut AND :fin")
    BigDecimal sumAchatsPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
