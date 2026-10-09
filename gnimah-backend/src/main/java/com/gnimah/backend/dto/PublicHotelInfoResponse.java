package com.gnimah.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicHotelInfoResponse {
    private long totalChambres;
    /** Version de l'application (build.gradle). */
    private String version;
    /** Horodatage du build : identifie le déploiement en service. */
    private String dateBuild;
}
