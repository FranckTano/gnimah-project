package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // « First » : ni le téléphone ni le numéro seul ne sont uniques en base ; une
    // recherche exacte ne doit pas échouer parce que deux fiches les partagent.
    Optional<Client> findFirstByTelephoneOrderByIdDesc(String telephone);
    Optional<Client> findFirstByNumeroPieceOrderByIdDesc(String numeroPiece);
    Optional<Client> findFirstByTypePieceAndNumeroPiece(String typePiece, String numeroPiece);

    /**
     * Recherche libre sur le nom, le prénom, le téléphone ou la pièce. Le
     * téléphone est aussi comparé sans espaces ({@code compact}) : « 0544556677 »
     * doit retrouver le client enregistré « +225 05 44 55 66 77 ».
     */
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.telephone LIKE CONCAT('%', :search, '%') OR " +
           "REPLACE(c.telephone, ' ', '') LIKE CONCAT('%', :compact, '%') OR " +
           "LOWER(c.numeroPiece) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Client> searchClients(@Param("search") String search, @Param("compact") String compact, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.nbSejours >= :minSejours ORDER BY c.nbSejours DESC")
    List<Client> findClientsRecurrents(@Param("minSejours") int minSejours);

    long countByNbSejoursGreaterThanEqual(int minSejours);
}
