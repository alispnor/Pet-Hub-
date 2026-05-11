package com.alispnor.pethub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    void getProdutosPublicos_200_semAuth() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isOk());
    }

    @Test
    void getCategoriasPublicas_200_semAuth() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoint_clienteAutenticado_403() throws Exception {
        var token = AuthHelper.loginCliente(mockMvc, objectMapper);
        var body = Map.of("nome", "Tentativa", "slug", "tentativa", "ordem", 0);
        mockMvc.perform(post("/api/v1/admin/catalog/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_semAuth_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalog/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalido_401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
