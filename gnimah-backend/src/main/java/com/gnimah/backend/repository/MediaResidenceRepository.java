package com.gnimah.backend.repository;

import com.gnimah.backend.entity.MediaResidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MediaResidenceRepository extends JpaRepository<MediaResidence, Long> {

    /** Galerie publique : uniquement les visuels actifs, dans l'ordre voulu. */
    List<MediaResidence> findByActifTrueOrderByPositionAscIdAsc();

    /** Vue d'administration : tout, y compris les visuels masqués. */
    List<MediaResidence> findAllByOrderByPositionAscIdAsc();

    Optional<MediaResidence> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    /** Prochaine position libre, pour poser un nouvel envoi en fin de galerie. */
    @Query("SELECT COALESCE(MAX(m.position), -1) + 1 FROM MediaResidence m")
    int prochainePosition();

    /**
     * Retire la mise en avant partout ailleurs : un seul visuel peut porter les
     * bandeaux d'accueil à la fois.
     */
    @Query("UPDATE MediaResidence m SET m.misEnAvant = false WHERE m.id <> :id")
    @org.springframework.data.jpa.repository.Modifying
    void retirerMiseEnAvantSauf(Long id);
}
