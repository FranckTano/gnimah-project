package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.enums.TypeCaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Long> {

    List<Caisse> findByActifTrueOrderByNomAsc();

    Optional<Caisse> findByNom(String nom);

    Optional<Caisse> findFirstByTypeAndActifTrueOrderByIdAsc(TypeCaisse type);

    @Query("SELECT COALESCE(SUM(c.solde), 0) FROM Caisse c WHERE c.actif = true")
    BigDecimal sumSoldes();
}
