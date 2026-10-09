package com.gnimah.backend.repository;

import com.gnimah.backend.entity.CommandeRestaurant;
import com.gnimah.backend.entity.enums.StatutCommande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommandeRestaurantRepository extends JpaRepository<CommandeRestaurant, Long> {

    Page<CommandeRestaurant> findAllByOrderByDateCommandeDesc(Pageable pageable);

    List<CommandeRestaurant> findByStatutOrderByDateCommandeDesc(StatutCommande statut);

    List<CommandeRestaurant> findBySejourIdOrderByDateCommandeDesc(Long sejourId);

    @Query("SELECT c FROM CommandeRestaurant c WHERE c.statut IN " +
           "(com.gnimah.backend.entity.enums.StatutCommande.OUVERTE, " +
           " com.gnimah.backend.entity.enums.StatutCommande.ENVOYEE_CUISINE, " +
           " com.gnimah.backend.entity.enums.StatutCommande.SERVIE) " +
           "ORDER BY c.dateCommande DESC")
    List<CommandeRestaurant> findEnCours();

    @Query("SELECT COALESCE(SUM(c.montantNet), 0) FROM CommandeRestaurant c " +
           "WHERE c.statut = com.gnimah.backend.entity.enums.StatutCommande.PAYEE " +
           "AND c.dateCommande >= :debut AND c.dateCommande < :fin")
    BigDecimal sumChiffreAffairesPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(c) FROM CommandeRestaurant c WHERE c.dateCommande >= :debut AND c.dateCommande < :fin")
    long countPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** (dateCommande, montantNet) des additions réglées de la période, pour la courbe jour par jour. */
    @Query("SELECT c.dateCommande, c.montantNet FROM CommandeRestaurant c " +
           "WHERE c.statut = com.gnimah.backend.entity.enums.StatutCommande.PAYEE " +
           "AND c.dateCommande >= :debut AND c.dateCommande < :fin")
    List<Object[]> findVentesPayeesPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Palmarès des plats vendus sur une période : article, quantité, chiffre d'affaires. */
    @Query("SELECT l.designation, SUM(l.quantite), COALESCE(SUM(l.montant), 0) " +
           "FROM LigneCommande l JOIN l.commande c " +
           "WHERE c.statut <> com.gnimah.backend.entity.enums.StatutCommande.ANNULEE " +
           "AND c.dateCommande >= :debut AND c.dateCommande < :fin " +
           "GROUP BY l.designation ORDER BY SUM(l.quantite) DESC")
    List<Object[]> findTopArticles(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT MAX(CAST(SUBSTRING(c.numero, LENGTH(c.numero) - 3, 4) AS int)) FROM CommandeRestaurant c " +
           "WHERE c.numero LIKE CONCAT(:prefix, '%')")
    Integer findMaxSequence(@Param("prefix") String prefix);
}
