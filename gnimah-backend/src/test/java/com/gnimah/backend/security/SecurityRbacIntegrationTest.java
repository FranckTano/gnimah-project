package com.gnimah.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnimah.backend.dto.auth.LoginRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie que les frontières de rôle définies dans SecurityConfig / les @PreAuthorize sont
 * réellement appliquées de bout en bout (chaîne de filtres + JWT + method security), pas seulement
 * "en théorie" dans le code. Un seul login par rôle (@BeforeAll, une fois pour toute la classe) pour
 * rester très en dessous du seuil du RateLimitFilter, qui est un singleton partagé par tout le run de tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SecurityRbacIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String directeurToken;
    private String responsableToken;
    private String agentToken;

    @BeforeAll
    void loginAllRoles() throws Exception {
        adminToken = login("admin", "Admin@2026");
        directeurToken = login("directeur", "Directeur@2026");
        responsableToken = login("responsable", "Responsable@2026");
        agentToken = login("agent", "Agent@2026");
    }

    private String login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    // --- Authentification ---

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("agent");
        request.setPassword("MauvaisMotDePasse");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/clients").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // --- Statistiques : réservé Directeur/Admin ---

    @Test
    void kpiDashboard_asAgent_isForbidden() throws Exception {
        mockMvc.perform(get("/kpi/dashboard/today").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void kpiDashboard_asResponsable_isForbidden() throws Exception {
        mockMvc.perform(get("/kpi/dashboard/today").header("Authorization", "Bearer " + responsableToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void kpiDashboard_asDirecteur_isAllowed() throws Exception {
        mockMvc.perform(get("/kpi/dashboard/today").header("Authorization", "Bearer " + directeurToken))
                .andExpect(status().isOk());
    }

    // --- Gestion des chambres : création réservée Directeur/Admin ---

    @Test
    void createChambre_asAgent_isForbidden() throws Exception {
        String body = """
                {"numero":"RBAC-TEST-1","type":"STANDARD","capacite":2,"tarifPassage":5000,"tarifNuitee":15000}
                """;
        mockMvc.perform(post("/chambres").header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createChambre_asDirecteur_isAllowed() throws Exception {
        String body = """
                {"numero":"RBAC-TEST-2","type":"STANDARD","capacite":2,"tarifPassage":5000,"tarifNuitee":15000}
                """;
        mockMvc.perform(post("/chambres").header("Authorization", "Bearer " + directeurToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // --- Housekeeping : création réservée Responsable/Directeur/Admin, pas Agent ---

    @Test
    void createTacheEntretien_asAgent_isForbidden() throws Exception {
        String body = """
                {"titre":"Tâche RBAC","type":"NETTOYAGE","description":"Test","agentId":1,"priorite":"NORMALE"}
                """;
        mockMvc.perform(post("/entretien").header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTacheEntretien_asResponsable_isAllowed() throws Exception {
        long agentId = extractUserId(agentToken);
        String body = "{\"titre\":\"Tâche RBAC\",\"type\":\"NETTOYAGE\",\"description\":\"Test\",\"agentId\":" + agentId + ",\"priorite\":\"NORMALE\"}";
        mockMvc.perform(post("/entretien").header("Authorization", "Bearer " + responsableToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private long extractUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/utilisateurs").header("Authorization", "Bearer " + adminToken))
                .andReturn();
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        for (var node : json) {
            if (node.get("username").asText().equals("agent")) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Compte agent introuvable");
    }

    // --- Administration du personnel : réservée Admin (lecture ouverte au Directeur) ---

    @Test
    void listUtilisateurs_asAgent_isForbidden() throws Exception {
        mockMvc.perform(get("/utilisateurs").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUtilisateurs_asDirecteur_isAllowedReadOnly() throws Exception {
        mockMvc.perform(get("/utilisateurs").header("Authorization", "Bearer " + directeurToken))
                .andExpect(status().isOk());
    }

    @Test
    void createUtilisateur_asDirecteur_isForbidden() throws Exception {
        String body = """
                {"username":"rbac-test-user","password":"Test@12345","nom":"Test","prenom":"RBAC","role":"AGENT","actif":true}
                """;
        mockMvc.perform(post("/utilisateurs").header("Authorization", "Bearer " + directeurToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUtilisateur_asAdmin_isAllowed() throws Exception {
        String body = """
                {"username":"rbac-test-user2","password":"Test@12345","nom":"Test","prenom":"RBAC","role":"AGENT","actif":true}
                """;
        mockMvc.perform(post("/utilisateurs").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
