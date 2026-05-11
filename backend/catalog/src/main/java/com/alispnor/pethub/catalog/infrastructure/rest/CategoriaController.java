package com.alispnor.pethub.catalog.infrastructure.rest;

import com.alispnor.pethub.catalog.application.dto.CategoriaRequest;
import com.alispnor.pethub.catalog.application.dto.CategoriaResponse;
import com.alispnor.pethub.catalog.application.usecase.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Catalog / Categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping("/api/v1/catalog/categories")
    @Operation(summary = "Lista categorias ativas (público)")
    public List<CategoriaResponse> listarPublico() {
        return categoriaService.listar(true);
    }

    @GetMapping("/api/v1/admin/catalog/categories")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE','OPERADOR')")
    @Operation(summary = "Lista todas as categorias incluindo inativas (admin)")
    public List<CategoriaResponse> listarAdmin() {
        return categoriaService.listar(false);
    }

    @PostMapping("/api/v1/admin/catalog/categories")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Cria categoria")
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criar(request));
    }

    @PutMapping("/api/v1/admin/catalog/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Atualiza categoria")
    public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.atualizar(id, request);
    }

    @DeleteMapping("/api/v1/admin/catalog/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Desativa categoria (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        categoriaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
