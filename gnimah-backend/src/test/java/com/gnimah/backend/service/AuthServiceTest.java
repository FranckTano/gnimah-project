package com.gnimah.backend.service;

import com.gnimah.backend.dto.auth.ChangePasswordRequest;
import com.gnimah.backend.dto.auth.LoginRequest;
import com.gnimah.backend.dto.auth.LoginResponse;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.Role;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.repository.UtilisateurRepository;
import com.gnimah.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(1L).nom("Réception").prenom("Agent").username("agent")
                .email("agent@gnimah.com").password("$2a$12$hash").role(Role.AGENT).actif(true)
                .build();
    }

    @Test
    void login_withValidCredentials_returnsTokenAndUserInfo() {
        LoginRequest request = new LoginRequest();
        request.setUsername("agent");
        request.setPassword("Agent@2026");

        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.of(utilisateur));
        UserDetails userDetails = User.builder().username("agent").password("$2a$12$hash")
                .authorities(List.of()).build();
        when(userDetailsService.loadUserByUsername("agent")).thenReturn(userDetails);
        when(jwtUtil.generateToken(eq("agent"), anyMap())).thenReturn("signed.jwt.token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("signed.jwt.token");
        assertThat(response.getUsername()).isEqualTo("agent");
        assertThat(response.getRole()).isEqualTo("AGENT");
        verify(authenticationManager).authenticate(any());
        verify(utilisateurRepository).updateLastLogin(eq(1L), any());
        verify(auditService).log(eq(utilisateur), eq("LOGIN"), eq("Utilisateur"), eq(1L), anyString());
    }

    @Test
    void login_withInvalidCredentials_propagatesBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("agent");
        request.setPassword("wrong-password");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtUtil);
        verify(utilisateurRepository, never()).updateLastLogin(any(), any());
    }

    @Test
    void changePassword_withWrongOldPassword_throwsBusinessException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("wrong-old-password");
        request.setNouveauMotDePasse("NewPassword@2026");

        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("wrong-old-password", utilisateur.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("agent", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incorrect");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void changePassword_withCorrectOldPassword_encodesAndSavesNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("Agent@2026");
        request.setNouveauMotDePasse("NewPassword@2026");

        when(utilisateurRepository.findByUsername("agent")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("Agent@2026", utilisateur.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword@2026")).thenReturn("$2a$12$newhash");

        authService.changePassword("agent", request);

        assertThat(utilisateur.getPassword()).isEqualTo("$2a$12$newhash");
        verify(utilisateurRepository).save(utilisateur);
        verify(auditService).log(eq(utilisateur), eq("CHANGE_PASSWORD"), anyString(), eq(1L), anyString());
    }
}
