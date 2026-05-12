package com.alispnor.pethub.customer.application.usecase;

import com.alispnor.pethub.common.exception.ConflictException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.application.dto.PerfilResponse;
import com.alispnor.pethub.customer.application.dto.SetCpfRequest;
import com.alispnor.pethub.customer.application.dto.UpdatePerfilRequest;
import com.alispnor.pethub.customer.domain.entity.PerfilCliente;
import com.alispnor.pethub.customer.infrastructure.persistence.PerfilClienteRepository;
import com.alispnor.pethub.customer.infrastructure.security.CpfHasher;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilClienteRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final CpfHasher cpfHasher;

    @Transactional(readOnly = true)
    public PerfilResponse getOrCreate(Long usuarioId) {
        log.info("Buscando perfil do usuário {}", usuarioId);
        var usuario = loadUsuario(usuarioId);
        var perfil = perfilRepository.findByUsuarioId(usuarioId).orElse(null);
        var response = toResponse(usuario, perfil);
        log.info("Perfil retornado para usuário {} (existente={})", usuarioId, perfil != null);
        return response;
    }

    @Transactional
    public PerfilResponse update(Long usuarioId, UpdatePerfilRequest request) {
        log.info("Atualizando perfil do usuário {}", usuarioId);
        var usuario = loadUsuario(usuarioId);
        var perfil = perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> PerfilCliente.builder().usuario(usuario).build());

        if (request.dataNascimento() != null) {
            perfil.setDataNascimento(request.dataNascimento());
        }
        if (request.genero() != null) {
            perfil.setGenero(request.genero());
        }
        if (request.telefoneAdicional() != null) {
            perfil.setTelefoneAdicional(request.telefoneAdicional());
        }
        if (request.aceiteTermos() != null && request.aceiteTermos() && !perfil.isAceiteTermos()) {
            perfil.setAceiteTermos(true);
            perfil.setAceiteTermosEm(LocalDateTime.now());
        }
        if (request.aceiteMarketing() != null) {
            perfil.setAceiteMarketing(request.aceiteMarketing());
        }

        var saved = perfilRepository.save(perfil);
        var response = toResponse(usuario, saved);
        log.info("Perfil do usuário {} atualizado (perfilId={})", usuarioId, saved.getId());
        return response;
    }

    @Transactional
    public PerfilResponse setCpf(Long usuarioId, SetCpfRequest request) {
        log.info("Definindo CPF do usuário {}", usuarioId);
        var usuario = loadUsuario(usuarioId);
        var cpfDigits = onlyDigits(request.cpf());
        var hash = cpfHasher.hash(cpfDigits);

        perfilRepository.findByCpfHash(hash).ifPresent(existing -> {
            if (!existing.getUsuario().getId().equals(usuarioId)) {
                throw new ConflictException("CPF já cadastrado para outro usuário");
            }
        });

        var perfil = perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> PerfilCliente.builder().usuario(usuario).build());

        if (perfil.getCpfHash() != null && !perfil.getCpfHash().equals(hash)) {
            throw new ConflictException("CPF já definido para este usuário e não pode ser alterado");
        }

        perfil.setCpfCriptografado(cpfDigits);
        perfil.setCpfHash(hash);
        var saved = perfilRepository.save(perfil);
        var response = toResponse(usuario, saved);
        log.info("CPF definido para usuário {} (perfilId={})", usuarioId, saved.getId());
        return response;
    }

    private Usuario loadUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário " + usuarioId + " não encontrado"));
    }

    private PerfilResponse toResponse(Usuario usuario, PerfilCliente perfil) {
        if (perfil == null) {
            return new PerfilResponse(
                    null,
                    usuario.getNome(),
                    usuario.getEmail(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    false
            );
        }
        return new PerfilResponse(
                perfil.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                maskCpf(perfil.getCpfCriptografado()),
                perfil.getDataNascimento(),
                perfil.getGenero(),
                perfil.getTelefoneAdicional(),
                perfil.isAceiteTermos(),
                perfil.isAceiteMarketing()
        );
    }

    private String maskCpf(String cpfPlain) {
        if (cpfPlain == null || cpfPlain.length() != 11) {
            return null;
        }
        return "***." + cpfPlain.substring(3, 6) + "." + cpfPlain.substring(6, 9) + "-**";
    }

    private String onlyDigits(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}
