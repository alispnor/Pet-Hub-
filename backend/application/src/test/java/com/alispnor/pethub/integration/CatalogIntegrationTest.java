package com.alispnor.pethub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends BaseIntegrationTest {

    @Test
    void listarProdutos_paginadoESucesso() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }

    @Test
    void getProdutoPorSku_existente_200() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products/COLLAR-PRO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("COLLAR-PRO-001"))
                .andExpect(jsonPath("$.preco").value(799.90));
    }

    @Test
    void getProdutoPorSku_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products/NAO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void filtrarProdutosPorCategoria_200() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products").param("categoria", "smart-collars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoriaSlug").value("smart-collars"));
    }

    @Test
    void buscarProdutos_porQuery_200() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products/search").param("q", "collar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }

    @Test
    void criarProduto_comoAdmin_201() throws Exception {
        var token = AuthHelper.loginAdmin(mockMvc, objectMapper);
        var produto = Map.of(
                "sku", "TEST-" + UUID.randomUUID().toString().substring(0, 8),
                "nome", "Produto Teste",
                "descricaoCurta", "Para teste de integração",
                "marca", "TestBrand",
                "categoriaId", 1L,
                "pesoKg", new BigDecimal("0.500"),
                "ncm", "85176259",
                "origem", "NACIONAL",
                "destacado", false,
                "precoInicial", new BigDecimal("99.90")
        );
        mockMvc.perform(post("/api/v1/admin/catalog/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Produto Teste"))
                .andExpect(jsonPath("$.preco").value(99.90));
    }
}
