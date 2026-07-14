package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findByNumero(String numero);
    Optional<Facture> findBySejourId(Long sejourId);
}
