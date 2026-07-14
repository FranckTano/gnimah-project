package com.gnimah.backend.repository;

import com.gnimah.backend.entity.TacheEntretien;
import com.gnimah.backend.entity.enums.StatutTache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TacheEntretienRepository extends JpaRepository<TacheEntretien, Long> {
    List<TacheEntretien> findByChambreId(Long chambreId);
    List<TacheEntretien> findByStatut(StatutTache statut);
    List<TacheEntretien> findByAgentId(Long agentId);
    List<TacheEntretien> findByStatutIn(List<StatutTache> statuts);
    long countByStatut(StatutTache statut);
}
