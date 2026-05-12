package com.alispnor.pethub.customer.application.usecase;

import com.alispnor.pethub.common.exception.ForbiddenException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.application.dto.CreatePetRequest;
import com.alispnor.pethub.customer.application.dto.PetResponse;
import com.alispnor.pethub.customer.application.dto.UpdatePetRequest;
import com.alispnor.pethub.customer.domain.entity.PerfilCliente;
import com.alispnor.pethub.customer.domain.entity.Pet;
import com.alispnor.pethub.customer.infrastructure.persistence.PerfilClienteRepository;
import com.alispnor.pethub.customer.infrastructure.persistence.PetRepository;
import com.alispnor.pethub.customer.infrastructure.storage.PhotoStorage;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private static final String PHOTO_NAMESPACE = "pets";

    private final PetRepository petRepository;
    private final PerfilClienteRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PhotoStorage photoStorage;

    @Transactional(readOnly = true)
    public List<PetResponse> listar(Long usuarioId) {
        log.info("Listando pets do usuário {}", usuarioId);
        var perfilId = perfilIdOptional(usuarioId);
        if (perfilId == null) {
            log.info("Usuário {} sem perfil — retornando lista vazia", usuarioId);
            return List.of();
        }
        var pets = petRepository.findAllByPerfilClienteIdOrderByNomeAsc(perfilId).stream()
                .map(this::toResponse)
                .toList();
        log.info("Encontrados {} pets para usuário {}", pets.size(), usuarioId);
        return pets;
    }

    @Transactional(readOnly = true)
    public PetResponse buscar(Long usuarioId, Long petId) {
        log.info("Buscando pet {} para usuário {}", petId, usuarioId);
        var pet = loadOwnedPet(usuarioId, petId);
        var response = toResponse(pet);
        log.info("Pet {} retornado (usuário {})", petId, usuarioId);
        return response;
    }

    @Transactional
    public PetResponse criar(Long usuarioId, CreatePetRequest request) {
        log.info("Criando pet para usuário {}: nome={}, especie={}", usuarioId, request.nome(), request.especie());
        var perfil = perfilOrCreate(usuarioId);

        var pet = Pet.builder()
                .perfilCliente(perfil)
                .nome(request.nome())
                .especie(request.especie())
                .raca(request.raca())
                .dataNascimento(request.dataNascimento())
                .pesoKg(request.pesoKg())
                .porte(request.porte())
                .observacoes(request.observacoes())
                .build();
        var saved = petRepository.save(pet);
        var response = toResponse(saved);
        log.info("Pet {} criado para usuário {}", saved.getId(), usuarioId);
        return response;
    }

    @Transactional
    public PetResponse atualizar(Long usuarioId, Long petId, UpdatePetRequest request) {
        log.info("Atualizando pet {} do usuário {}", petId, usuarioId);
        var pet = loadOwnedPet(usuarioId, petId);

        if (request.nome() != null) pet.setNome(request.nome());
        if (request.especie() != null) pet.setEspecie(request.especie());
        if (request.raca() != null) pet.setRaca(request.raca());
        if (request.dataNascimento() != null) pet.setDataNascimento(request.dataNascimento());
        if (request.pesoKg() != null) pet.setPesoKg(request.pesoKg());
        if (request.porte() != null) pet.setPorte(request.porte());
        if (request.observacoes() != null) pet.setObservacoes(request.observacoes());

        var saved = petRepository.save(pet);
        var response = toResponse(saved);
        log.info("Pet {} atualizado (usuário {})", petId, usuarioId);
        return response;
    }

    @Transactional
    public void remover(Long usuarioId, Long petId) {
        log.info("Removendo pet {} do usuário {}", petId, usuarioId);
        var pet = loadOwnedPet(usuarioId, petId);
        petRepository.delete(pet);
        log.info("Pet {} removido (usuário {})", petId, usuarioId);
    }

    @Transactional
    public PetResponse uploadFoto(Long usuarioId, Long petId, MultipartFile file) {
        log.info("Upload de foto para pet {} (usuário {}, size={} bytes)", petId, usuarioId, file.getSize());
        var pet = loadOwnedPet(usuarioId, petId);
        var url = photoStorage.store(PHOTO_NAMESPACE, pet.getId(), file);
        pet.setFotoUrl(url);
        var saved = petRepository.save(pet);
        var response = toResponse(saved);
        log.info("Foto do pet {} atualizada: {}", petId, url);
        return response;
    }

    private Pet loadOwnedPet(Long usuarioId, Long petId) {
        var pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet " + petId + " não encontrado"));
        var ownerUserId = pet.getPerfilCliente().getUsuario().getId();
        if (!ownerUserId.equals(usuarioId)) {
            log.warn("Tentativa de acesso ao pet {} pelo usuário {} (dono é {})", petId, usuarioId, ownerUserId);
            throw new ForbiddenException("Pet pertence a outro usuário");
        }
        return pet;
    }

    private Long perfilIdOptional(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId).map(PerfilCliente::getId).orElse(null);
    }

    private PerfilCliente perfilOrCreate(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    var usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário " + usuarioId + " não encontrado"));
                    return perfilRepository.save(PerfilCliente.builder().usuario(usuario).build());
                });
    }

    private PetResponse toResponse(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getNome(),
                pet.getEspecie(),
                pet.getRaca(),
                pet.getDataNascimento(),
                pet.getPesoKg(),
                pet.getPorte(),
                pet.getObservacoes(),
                pet.getFotoUrl()
        );
    }
}
