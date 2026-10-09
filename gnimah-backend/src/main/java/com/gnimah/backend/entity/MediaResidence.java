package com.gnimah.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Un visuel de la galerie « La Résidence ».
 *
 * Seules les métadonnées sont en base : le fichier lui-même vit sur Cloudinary.
 * {@code publicId} garde l'identifiant distant afin de pouvoir supprimer l'objet
 * en même temps que la ligne, et d'éviter les fichiers orphelins qui
 * consommeraient du quota sans plus être affichés nulle part.
 */
@Entity
@Table(name = "medias_residence")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediaResidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Builder.Default
    @Column(nullable = false, length = 60)
    private String categorie = "Chambres";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    /** Version allégée pour les grilles ; à défaut, l'affichage retombe sur {@link #url}. */
    @Column(name = "url_vignette", columnDefinition = "TEXT")
    private String urlVignette;

    /** Identifiant Cloudinary. Nul pour une URL externe saisie à la main. */
    @Column(name = "public_id", length = 255)
    private String publicId;

    /** Ordre d'affichage, les plus petits d'abord. */
    @Builder.Default
    @Column(nullable = false)
    private int position = 0;

    /** Permet de masquer un visuel sans le supprimer. */
    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    /** Visuel utilisé pour les bandeaux d'accueil et l'affiche de la vidéo. */
    @Builder.Default
    @Column(name = "mis_en_avant", nullable = false)
    private boolean misEnAvant = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
