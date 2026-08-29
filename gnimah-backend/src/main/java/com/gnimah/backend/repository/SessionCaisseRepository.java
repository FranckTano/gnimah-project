package com.gnimah.backend.repository;

import com.gnimah.backend.entity.SessionCaisse;
import com.gnimah.backend.entity.enums.StatutSessionCaisse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    Optional<SessionCaisse> findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(Long caisseId, StatutSessionCaisse statut);

    List<SessionCaisse> findByStatutOrderByDateOuvertureDesc(StatutSessionCaisse statut);

    Page<SessionCaisse> findByCaisseIdOrderByDateOuvertureDesc(Long caisseId, Pageable pageable);

    /** Dernière vacation ouverte par un agent donné, quelle que soit la caisse. */
    Optional<SessionCaisse> findFirstByCaissierIdAndStatutOrderByDateOuvertureDesc(Long caissierId, StatutSessionCaisse statut);
}
