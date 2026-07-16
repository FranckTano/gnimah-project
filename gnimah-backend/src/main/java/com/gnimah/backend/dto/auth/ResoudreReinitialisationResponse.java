package com.gnimah.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResoudreReinitialisationResponse {
    private String username;
    private String nouveauMotDePasse;
}
