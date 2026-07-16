package com.gnimah.backend.entity;

import com.gnimah.backend.entity.enums.EtatChambre;
import com.gnimah.backend.entity.enums.TypeChambre;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chambres")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chambre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(length = 150)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeChambre type = TypeChambre.STANDARD;

    @Column(nullable = false)
    private int capacite = 2;

    @Column(name = "tarif_passage", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifPassage;

    @Column(name = "tarif_nuitee", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifNuitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtatChambre etat = EtatChambre.LIBRE;

    private int etage = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String equipements;

    @Column(length = 30)
    private String vue;

    @Column(columnDefinition = "TEXT")
    private String observations;

    /** Comma-separated photo URLs — no upload pipeline yet, structure only. */
    @Column(columnDefinition = "TEXT")
    private String photos;

    @Column(nullable = false)
    private boolean actif = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isDisponible() {
        return etat == EtatChambre.LIBRE;
    }
}
