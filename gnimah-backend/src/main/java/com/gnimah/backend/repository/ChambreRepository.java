package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Chambre;
import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.TypeChambre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChambreRepository extends JpaRepository<Chambre, Long> {

    Optional<Chambre> findByNumero(String numero);
    List<Chambre> findByEtatAndActifTrue(EtatChambre etat);
    List<Chambre> findByActifTrue();
    List<Chambre> findByTypeAndEtatAndActifTrue(TypeChambre type, EtatChambre etat);
    long countByEtat(EtatChambre etat);
    long countByActifTrue();

    @Query("SELECT c FROM Chambre c WHERE c.actif = true AND c.etat = 'LIBRE' " +
           "AND c.id NOT IN (" +
           "SELECT r.chambre.id FROM Reservation r WHERE r.statut IN ('EN_ATTENTE', 'CONFIRMEE') " +
           "AND r.dateArrivee < :dateFin AND r.dateDepart > :dateDebut)")
    List<Chambre> findChambresDisponibles(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);
}
