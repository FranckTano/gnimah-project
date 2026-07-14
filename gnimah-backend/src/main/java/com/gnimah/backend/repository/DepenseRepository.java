package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Depense;
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

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.dateDepense BETWEEN :debut AND :fin")
    BigDecimal sumDepensesPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
