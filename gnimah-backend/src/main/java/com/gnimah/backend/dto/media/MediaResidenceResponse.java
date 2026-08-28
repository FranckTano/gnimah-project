package com.gnimah.backend.dto.media;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaResidenceResponse {
    private Long id;
    private String titre;
    private String categorie;
    private String url;
    private String urlVignette;
    private String publicId;
    private int position;
    private boolean actif;
    private boolean misEnAvant;
    /** Vrai si le fichier est hébergé par nous : la suppression l'effacera aussi. */
    private boolean heberge;
    private LocalDateTime createdAt;
}
