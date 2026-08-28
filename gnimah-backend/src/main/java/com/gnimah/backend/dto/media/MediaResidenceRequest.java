package com.gnimah.backend.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Métadonnées éditables d'un visuel de la galerie. */
@Data
public class MediaResidenceRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères")
    private String titre;

    @NotBlank(message = "La catégorie est obligatoire")
    @Size(max = 60)
    private String categorie;

    /**
     * Obligatoire seulement à la création par URL externe : lors d'un envoi de
     * fichier, le service la renseigne à partir de la réponse de Cloudinary.
     */
    private String url;

    private String urlVignette;

    private Integer position;

    private Boolean actif;

    private Boolean misEnAvant;

    /**
     * Identifiant Cloudinary, renseigné uniquement quand le navigateur a
     * téléversé le fichier lui-même. Validé côté serveur avant enregistrement.
     */
    private String publicId;
}
