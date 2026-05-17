package com.alispnor.pethub.catalog.infrastructure.rest;

import com.alispnor.pethub.catalog.application.dto.AdicionarImagemRequest;
import com.alispnor.pethub.catalog.application.dto.DefinirPrecoRequest;
import com.alispnor.pethub.catalog.application.dto.ProdutoAdminSummaryResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoDetailResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoImagemResponse;
import com.alispnor.pethub.catalog.application.dto.ProdutoRequest;
import com.alispnor.pethub.catalog.application.dto.ProdutoSummaryResponse;
import com.alispnor.pethub.catalog.application.usecase.ProdutoService;
import com.alispnor.pethub.common.dto.PageableResponse;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Catalog / Produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final CurrentUserProvider currentUserProvider;

    // ─── Público (storefront) ──────────────────────────────────

    @GetMapping("/api/v1/catalog/products")
    @Operation(summary = "Lista produtos ativos paginado, com filtro de categoria")
    public PageableResponse<ProdutoSummaryResponse> listar(
            @RequestParam(required = false) String categoria,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageableResponse.of(produtoService.listar(categoria, pageable));
    }

    @GetMapping("/api/v1/catalog/products/search")
    @Operation(summary = "Busca produtos por nome/descrição/marca")
    public PageableResponse<ProdutoSummaryResponse> buscar(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageableResponse.of(produtoService.buscar(q, pageable));
    }

    @GetMapping("/api/v1/catalog/products/{sku}")
    @Operation(summary = "Detalhe de um produto pelo SKU")
    public ProdutoDetailResponse buscarPorSku(@PathVariable String sku) {
        return produtoService.buscarPorSku(sku);
    }

    // ─── Admin ────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/catalog/products")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE','OPERADOR')")
    @Operation(summary = "Lista produtos para o admin (incluindo inativos) com filtros e ordenação")
    public PageableResponse<ProdutoAdminSummaryResponse> listarAdmin(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PageableResponse.of(produtoService.listarAdmin(q, categoriaId, ativo, pageable));
    }

    @PostMapping("/api/v1/admin/catalog/products")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Cria produto + preço inicial")
    public ResponseEntity<ProdutoDetailResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        var autor = currentUserProvider.requireCurrent();
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(request, autor));
    }

    @PutMapping("/api/v1/admin/catalog/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Atualiza produto")
    public ProdutoDetailResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return produtoService.atualizar(id, request);
    }

    @PatchMapping("/api/v1/admin/catalog/products/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE','OPERADOR')")
    @Operation(summary = "Toggle ativo/inativo (soft delete / restore)")
    public ProdutoDetailResponse toggleAtivo(@PathVariable Long id) {
        return produtoService.toggleAtivo(id);
    }

    @PostMapping("/api/v1/admin/catalog/products/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Adiciona imagem ao produto (URL)")
    public ResponseEntity<ProdutoImagemResponse> adicionarImagem(@PathVariable Long id,
                                                                 @Valid @RequestBody AdicionarImagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.adicionarImagem(id, request));
    }

    @DeleteMapping("/api/v1/admin/catalog/products/{id}/images/{imgId}")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Remove imagem do produto")
    public ResponseEntity<Void> removerImagem(@PathVariable Long id, @PathVariable Long imgId) {
        produtoService.removerImagem(id, imgId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/catalog/products/{id}/price")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Define novo preço vigente (fecha o anterior)")
    public ProdutoDetailResponse definirPreco(@PathVariable Long id, @Valid @RequestBody DefinirPrecoRequest request) {
        var autor = currentUserProvider.requireCurrent();
        return produtoService.definirPreco(id, request, autor);
    }
}
