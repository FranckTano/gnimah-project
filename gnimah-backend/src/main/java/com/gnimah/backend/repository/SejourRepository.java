package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Sejour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Les requêtes de période prennent un intervalle semi-ouvert [debut, fin[ :
 * une journée se passe donc comme (jour 00:00, lendemain 00:00). Les arrivées
 * sont enregistrées à 00:00 ; avec des bornes incluses, celles du lendemain
 * seraient comptées deux fois — dans leur journée et dans la précédente.
 */
@Repository
public interface SejourRepository extends JpaRepository<Sejour, Long> {

    Optional<Sejour> findByNumeroRecu(String numeroRecu);
    Page<Sejour> findByClientId(Long clientId, Pageable pageable);
    boolean existsByClientId(Long clientId);
    List<Sejour> findByChambreIdAndStatut(Long chambreId, String statut);
    List<Sejour> findByStatut(String statut);

    @Query("SELECT s FROM Sejour s WHERE s.statut = 'EN_COURS'")
    List<Sejour> findSejoursEnCours();

    @Query("SELECT s FROM Sejour s WHERE s.dateSortie >= :debut AND s.dateSortie < :fin")
    List<Sejour> findDepartsJour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT s FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    List<Sejour> findArriveesPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(s.montantTotal), 0) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    BigDecimal sumChiffreAffaires(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(s.montantPaye), 0) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    BigDecimal sumEncaissements(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * (dateEntree, montantTotal, montantExtras) de chaque arrivée de la période.
     * Les courbes jour par jour s'agrègent en mémoire à partir de cette seule
     * requête, plutôt qu'au prix d'un aller-retour en base par jour affiché.
     */
    @Query("SELECT s.dateEntree, s.montantTotal, s.montantExtras FROM Sejour s " +
           "WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    List<Object[]> findMontantsParArrivee(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Recette de l'hébergement seul. Le total d'un séjour englobe désormais les
     * consommations annexes (restauration portée à la chambre, dégâts facturés) ;
     * les retrancher évite de compter deux fois ce que le module restauration
     * comptabilise déjà de son côté.
     */
    @Query("SELECT COALESCE(SUM(s.montantTotal - s.montantExtras), 0) FROM Sejour s " +
           "WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    BigDecimal sumRecetteHebergement(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(s.remiseMontant), 0) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    BigDecimal sumRemises(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Créances : ce que les clients encore présents restent devoir. */
    @Query("SELECT COALESCE(SUM(s.resteAPayer), 0) FROM Sejour s WHERE s.statut = 'EN_COURS'")
    BigDecimal sumCreancesEnCours();

    @Query("SELECT COUNT(s) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    long countSejoursPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(AVG(s.nbJours), 0) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin AND s.nbJours IS NOT NULL")
    Double avgDureeSejour(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(DISTINCT s.client.id) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin")
    long countClientsUniques(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT s.agent.id, COUNT(s), SUM(s.montantTotal) FROM Sejour s " +
           "WHERE s.dateEntree >= :debut AND s.dateEntree < :fin GROUP BY s.agent.id")
    List<Object[]> findPerformanceAgents(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT s.typeLocation, COUNT(s) FROM Sejour s WHERE s.dateEntree >= :debut AND s.dateEntree < :fin GROUP BY s.typeLocation")
    List<Object[]> countByTypeLocationPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MAX(CAST(SUBSTRING(s.numeroRecu, LENGTH(s.numeroRecu) - 3, 4) AS int)) FROM Sejour s " +
           "WHERE s.numeroRecu LIKE CONCAT(:prefix, '%')")
    Integer findMaxSequence(@Param("prefix") String prefix);
}
