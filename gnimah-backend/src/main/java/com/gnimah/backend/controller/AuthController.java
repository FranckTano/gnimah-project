package com.gnimah.backend.controller;

import com.gnimah.backend.dto.auth.ChangePasswordRequest;
import com.gnimah.backend.dto.auth.ForgotPasswordRequest;
import com.gnimah.backend.dto.auth.ForgotPasswordResponse;
import com.gnimah.backend.dto.auth.LoginRequest;
import com.gnimah.backend.dto.auth.LoginResponse;
import com.gnimah.backend.service.AuthService;
import com.gnimah.backend.service.PasswordResetRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetRequestService passwordResetRequestService;

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demander une réinitialisation de mot de passe (validée ensuite par un administrateur)")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetRequestService.demander(request.getUsername());
        return ResponseEntity.ok(ForgotPasswordResponse.builder()
                .message("Si ce compte existe, une demande de réinitialisation a été transmise à l'administrateur.")
                .build());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changer le mot de passe")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(username, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Informations utilisateur connecté")
    public ResponseEntity<String> me() {
        return ResponseEntity.ok(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
