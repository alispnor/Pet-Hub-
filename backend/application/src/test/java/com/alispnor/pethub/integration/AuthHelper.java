package com.alispnor.pethub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helper para obter tokens nos testes de integração.
 */
public final class AuthHelper {

    private AuthHelper() {
    }

    public static String loginAndGetAccessToken(MockMvc mockMvc, ObjectMapper mapper, String email, String senha) throws Exception {
        var body = mapper.writeValueAsString(Map.of("email", email, "senha", senha));
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = mapper.readTree(response);
        return json.get("accessToken").asText();
    }

    public static String loginAdmin(MockMvc mockMvc, ObjectMapper mapper) throws Exception {
        return loginAndGetAccessToken(mockMvc, mapper, "admin@pethub.com", "Admin@123");
    }

    public static String loginCliente(MockMvc mockMvc, ObjectMapper mapper) throws Exception {
        return loginAndGetAccessToken(mockMvc, mapper, "cliente@teste.com", "Cliente@123");
    }
}
