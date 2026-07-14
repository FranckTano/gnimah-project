package com.gnimah.backend.repository;

import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByUsername(String username);
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRole(Role role);
    List<Utilisateur> findByActifTrue();

    @Modifying
    @Query("UPDATE Utilisateur u SET u.lastLogin = :lastLogin WHERE u.id = :id")
    void updateLastLogin(Long id, LocalDateTime lastLogin);
}
