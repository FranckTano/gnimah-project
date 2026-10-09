package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Depense;
import com.gnimah.backend.entity.enums.FamilleDepense;
import com.gnimah.backend.entity.enums.StatutDepense;
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
public interface DepenseRepository extends JpaRepository<Depense, Long> {

    List<Depense> findByDateDepenseBetween(LocalDate debut, LocalDate fin);

    List<Depense> findByDateDepenseBetweenOrderByDateDepenseDesc(LocalDate debut, LocalDate fin);

    Page<Depense> findAllByOrderByDateDepenseDescIdDesc(Pageable pageable);

    List<Depense> findByStatutOrderByDateDepenseDesc(StatutDepense statut);

    List<Depense> findByFamilleAndDateDepenseBetweenOrderByDateDepenseDesc(
            FamilleDepense famille, LocalDate debut, LocalDate fin);

    /**
     * Total des charges d'une période. Les dépenses rejetées sont exclues :
     * elles n'ont jamais représenté une sortie d'argent.
     */
    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d " +
           "WHERE d.statut <> com.gnimah.backend.entity.enums.StatutDepense.REJETEE " +
           "AND d.dateDepense BETWEEN :debut AND :fin")
    BigDecimal sumDepensesPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /** (dateDepense, montant) des charges de la période hors rejets, pour la courbe jour par jour. */
    @Query("SELECT d.dateDepense, d.montant FROM Depense d " +
           "WHERE d.statut <> com.gnimah.backend.entity.enums.StatutDepense.REJETEE " +
           "AND d.dateDepense BETWEEN :debut AND :fin")
    List<Object[]> findMontantsPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /** Charges effectivement décaissées sur la période (hors engagements non réglés). */
    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d " +
           "WHERE d.statut = com.gnimah.backend.entity.enums.StatutDepense.PAYEE " +
           "AND d.dateDepense BETWEEN :debut AND :fin")
    BigDecimal sumDepensesPayeesPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT d.famille, COUNT(d), COALESCE(SUM(d.montant), 0) FROM Depense d " +
           "WHERE d.statut <> com.gnimah.backend.entity.enums.StatutDepense.REJETEE " +
           "AND d.dateDepense BETWEEN :debut AND :fin GROUP BY d.famille")
    List<Object[]> repartitionParFamille(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT d.fournisseur.nom, COUNT(d), COALESCE(SUM(d.montant), 0) FROM Depense d " +
           "WHERE d.fournisseur IS NOT NULL " +
           "AND d.statut <> com.gnimah.backend.entity.enums.StatutDepense.REJETEE " +
           "AND d.dateDepense BETWEEN :debut AND :fin " +
           "GROUP BY d.fournisseur.nom ORDER BY SUM(d.montant) DESC")
    List<Object[]> repartitionParFournisseur(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(d) FROM Depense d WHERE d.statut = com.gnimah.backend.entity.enums.StatutDepense.EN_ATTENTE")
    long countEnAttente();
}
