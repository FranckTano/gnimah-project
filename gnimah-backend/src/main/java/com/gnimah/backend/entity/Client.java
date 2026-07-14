package com.gnimah.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String civilite;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(name = "type_piece", nullable = false, length = 20)
    private String typePiece;

    @Column(name = "numero_piece", nullable = false, length = 50)
    private String numeroPiece;

    @Column(length = 100)
    private String nationalite = "Ivoirienne";

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "nb_sejours", nullable = false)
    private int nbSejours = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getNomComplet() {
        return civilite + " " + nom + (prenom != null ? " " + prenom : "");
    }

    public boolean isClientFidele() {
        return nbSejours >= 3;
    }
}
