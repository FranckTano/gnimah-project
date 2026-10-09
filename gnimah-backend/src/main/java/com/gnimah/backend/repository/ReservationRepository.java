package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Reservation;
import com.gnimah.backend.entity.enums.StatutReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByNumeroReservation(String numeroReservation);
    Page<Reservation> findByClientId(Long clientId, Pageable pageable);
    boolean existsByClientId(Long clientId);
    List<Reservation> findByStatut(StatutReservation statut);

    @Query("SELECT r FROM Reservation r WHERE r.dateArrivee >= :debut AND r.dateArrivee < :fin AND r.statut = :statut")
    List<Reservation> findArriveesPeriode(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            @Param("statut") StatutReservation statut);

    @Query("SELECT r FROM Reservation r WHERE r.chambre.id = :chambreId " +
           "AND r.statut IN ('EN_ATTENTE', 'CONFIRMEE') " +
           "AND r.dateArrivee < :dateFin AND r.dateDepart > :dateDebut")
    List<Reservation> findConflits(
            @Param("chambreId") Long chambreId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    long countByStatut(StatutReservation statut);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.createdAt >= :debut AND r.createdAt < :fin")
    long countReservationsPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MAX(CAST(SUBSTRING(r.numeroReservation, LENGTH(r.numeroReservation) - 3, 4) AS int)) " +
           "FROM Reservation r WHERE r.numeroReservation LIKE CONCAT(:prefix, '%')")
    Integer findMaxSequence(@Param("prefix") String prefix);

    /** Dates de prise des réservations de la période, pour la courbe jour par jour. */
    @Query("SELECT r.createdAt FROM Reservation r WHERE r.createdAt >= :debut AND r.createdAt < :fin")
    List<LocalDateTime> findDatesCreationPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}
