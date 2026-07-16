package com.gnimah.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Le nom d'utilisateur est requis")
    private String username;
}
