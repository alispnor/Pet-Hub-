package com.alispnor.pethub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerCliente_201_eAceitaLoginEmSeguida() throws Exception {
        var email = "novo-" + UUID.randomUUID() + "@teste.com";
        var register = Map.of(
                "nome", "Novo Cliente",
                "email", email,
                "senha", "SenhaForte123",
                "telefone", "+5511999998888"
        );

        mockMvc.perform(post("/api/v1/auth/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.tipo").value("CLIENTE"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENTE"));

        var login = Map.of("email", email, "senha", "SenhaForte123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void registerCliente_emailDuplicado_409() throws Exception {
        var dup = Map.of(
                "nome", "Duplicado",
                "email", "cliente@teste.com",
                "senha", "QualquerSenha123"
        );
        mockMvc.perform(post("/api/v1/auth/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"));
    }

    @Test
    void login_credencialErrada_401() throws Exception {
        var login = Map.of("email", "cliente@teste.com", "senha", "errada");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_semToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_comToken_200() throws Exception {
        var token = AuthHelper.loginCliente(mockMvc, objectMapper);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@teste.com"));
    }

    @Test
    void registerCliente_validacaoFalha_400() throws Exception {
        var invalid = Map.of("nome", "", "email", "nao-email", "senha", "curta");
        mockMvc.perform(post("/api/v1/auth/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }
}
