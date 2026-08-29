package com.gnimah.backend.repository;

import com.gnimah.backend.entity.CompteFinancier;
import com.gnimah.backend.entity.enums.TypeCompteFinancier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompteFinancierRepository extends JpaRepository<CompteFinancier, Long> {

    List<CompteFinancier> findByActifTrueOrderByLibelleAsc();

    List<CompteFinancier> findByTypeAndActifTrueOrderByLibelleAsc(TypeCompteFinancier type);
}
