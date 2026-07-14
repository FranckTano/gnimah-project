package com.gnimah.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String username;
    private String nom;
    private String prenom;
    private String role;
    private String email;
}
