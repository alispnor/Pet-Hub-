package com.alispnor.pethub.customer.infrastructure.rest;

import com.alispnor.pethub.customer.application.dto.CreatePetRequest;
import com.alispnor.pethub.customer.application.dto.PetResponse;
import com.alispnor.pethub.customer.application.dto.UpdatePetRequest;
import com.alispnor.pethub.customer.application.usecase.PetService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Gestão dos pets do cliente autenticado")
public class PetController {

    private final PetService petService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Lista todos os pets do cliente autenticado")
    public List<PetResponse> listar() {
        var current = currentUserProvider.requireCurrent();
        return petService.listar(current.id());
    }

    @PostMapping
    @Operation(summary = "Cria um novo pet")
    public ResponseEntity<PetResponse> criar(@Valid @RequestBody CreatePetRequest request) {
        var current = currentUserProvider.requireCurrent();
        var response = petService.criar(current.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna um pet específico (apenas se pertencer ao cliente)")
    public PetResponse buscar(@PathVariable Long id) {
        var current = currentUserProvider.requireCurrent();
        return petService.buscar(current.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um pet")
    public PetResponse atualizar(@PathVariable Long id, @Valid @RequestBody UpdatePetRequest request) {
        var current = currentUserProvider.requireCurrent();
        return petService.atualizar(current.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um pet")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        var current = currentUserProvider.requireCurrent();
        petService.remover(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload da foto do pet (campo multipart `file`)")
    public PetResponse uploadFoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        var current = currentUserProvider.requireCurrent();
        return petService.uploadFoto(current.id(), id, file);
    }
}
