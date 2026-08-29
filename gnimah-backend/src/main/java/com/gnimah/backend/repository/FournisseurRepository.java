package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Fournisseur;
import com.gnimah.backend.entity.enums.TypeFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    List<Fournisseur> findByActifTrueOrderByNomAsc();

    List<Fournisseur> findByTypeAndActifTrueOrderByNomAsc(TypeFournisseur type);

    List<Fournisseur> findByNomContainingIgnoreCase(String nom);
}
