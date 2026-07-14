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

    Optional<Client> findByTelephone(String telephone);
    Optional<Client> findByNumeroPiece(String numeroPiece);

    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.telephone LIKE CONCAT('%', :search, '%') OR " +
           "c.numeroPiece LIKE CONCAT('%', :search, '%')")
    Page<Client> searchClients(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.nbSejours >= :minSejours ORDER BY c.nbSejours DESC")
    List<Client> findClientsRecurrents(@Param("minSejours") int minSejours);

    long countByNbSejoursGreaterThanEqual(int minSejours);
}
