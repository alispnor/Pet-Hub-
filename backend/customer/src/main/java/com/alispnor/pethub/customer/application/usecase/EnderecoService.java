package com.alispnor.pethub.customer.application.usecase;

import com.alispnor.pethub.common.exception.ForbiddenException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.application.dto.CreateEnderecoRequest;
import com.alispnor.pethub.customer.application.dto.EnderecoResponse;
import com.alispnor.pethub.customer.application.dto.SetDefaultRequest;
import com.alispnor.pethub.customer.application.dto.UpdateEnderecoRequest;
import com.alispnor.pethub.customer.application.dto.ViaCepResponse;
import com.alispnor.pethub.customer.domain.entity.Endereco;
import com.alispnor.pethub.customer.domain.entity.PerfilCliente;
import com.alispnor.pethub.customer.infrastructure.integration.ViaCepClient;
import com.alispnor.pethub.customer.infrastructure.persistence.EnderecoRepository;
import com.alispnor.pethub.customer.infrastructure.persistence.PerfilClienteRepository;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final PerfilClienteRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ViaCepClient viaCepClient;

    @Transactional(readOnly = true)
    public ViaCepResponse consultarCep(String cep) {
        log.info("Consultando CEP {}", cep);
        var normalized = cep == null ? "" : cep.replaceAll("\\D", "");
        if (normalized.length() != 8) {
            throw new ResourceNotFoundException("CEP inválido: " + cep);
        }
        var response = viaCepClient.fetch(normalized);
        log.info("CEP {} resolvido para {}/{}", normalized, response.cidade(), response.uf());
        return response;
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listar(Long usuarioId) {
        log.info("Listando endereços do usuário {}", usuarioId);
        var perfil = perfilRepository.findByUsuarioId(usuarioId).orElse(null);
        if (perfil == null) {
            return List.of();
        }
        var enderecos = enderecoRepository.findAllByPerfilClienteIdOrderByApelidoAsc(perfil.getId()).stream()
                .map(this::toResponse)
                .toList();
        log.info("Encontrados {} endereços para usuário {}", enderecos.size(), usuarioId);
        return enderecos;
    }

    @Transactional
    public EnderecoResponse criar(Long usuarioId, CreateEnderecoRequest request) {
        log.info("Criando endereço '{}' para usuário {}", request.apelido(), usuarioId);
        var perfil = perfilOrCreate(usuarioId);
        var isFirst = enderecoRepository.countByPerfilClienteId(perfil.getId()) == 0;

        var entrega = Boolean.TRUE.equals(request.padraoEntrega()) || isFirst;
        var cobranca = Boolean.TRUE.equals(request.padraoCobranca()) || isFirst;

        var endereco = Endereco.builder()
                .perfilCliente(perfil)
                .apelido(request.apelido())
                .cep(request.cep())
                .logradouro(request.logradouro())
                .numero(request.numero())
                .complemento(request.complemento())
                .bairro(request.bairro())
                .cidade(request.cidade())
                .uf(request.uf())
                .pais("BR")
                .tipo(request.tipo())
                .padraoEntrega(false)
                .padraoCobranca(false)
                .ativo(true)
                .build();
        var saved = enderecoRepository.save(endereco);

        if (entrega) {
            enderecoRepository.clearOtherDefaultEntrega(perfil.getId(), saved.getId());
            saved.setPadraoEntrega(true);
        }
        if (cobranca) {
            enderecoRepository.clearOtherDefaultCobranca(perfil.getId(), saved.getId());
            saved.setPadraoCobranca(true);
        }
        enderecoRepository.flush();

        log.info("Endereço {} criado (usuário {}, entrega={}, cobranca={})",
                saved.getId(), usuarioId, entrega, cobranca);
        return toResponse(saved);
    }

    @Transactional
    public EnderecoResponse atualizar(Long usuarioId, Long enderecoId, UpdateEnderecoRequest request) {
        log.info("Atualizando endereço {} do usuário {}", enderecoId, usuarioId);
        var endereco = loadOwned(usuarioId, enderecoId);

        if (request.apelido() != null) endereco.setApelido(request.apelido());
        if (request.cep() != null) endereco.setCep(request.cep());
        if (request.logradouro() != null) endereco.setLogradouro(request.logradouro());
        if (request.numero() != null) endereco.setNumero(request.numero());
        if (request.complemento() != null) endereco.setComplemento(request.complemento());
        if (request.bairro() != null) endereco.setBairro(request.bairro());
        if (request.cidade() != null) endereco.setCidade(request.cidade());
        if (request.uf() != null) endereco.setUf(request.uf());
        if (request.tipo() != null) endereco.setTipo(request.tipo());
        if (request.ativo() != null) endereco.setAtivo(request.ativo());

        var saved = enderecoRepository.save(endereco);
        log.info("Endereço {} atualizado (usuário {})", enderecoId, usuarioId);
        return toResponse(saved);
    }

    @Transactional
    public void remover(Long usuarioId, Long enderecoId) {
        log.info("Removendo endereço {} do usuário {}", enderecoId, usuarioId);
        var endereco = loadOwned(usuarioId, enderecoId);
        enderecoRepository.delete(endereco);
        log.info("Endereço {} removido (usuário {})", enderecoId, usuarioId);
    }

    @Transactional
    public EnderecoResponse definirPadrao(Long usuarioId, Long enderecoId, SetDefaultRequest request) {
        log.info("Definindo padrão {} para endereço {} (usuário {})", request, enderecoId, usuarioId);
        var endereco = loadOwned(usuarioId, enderecoId);
        var perfilId = endereco.getPerfilCliente().getId();

        if (Boolean.TRUE.equals(request.padraoEntrega())) {
            enderecoRepository.clearOtherDefaultEntrega(perfilId, enderecoId);
            endereco.setPadraoEntrega(true);
        }
        if (Boolean.TRUE.equals(request.padraoCobranca())) {
            enderecoRepository.clearOtherDefaultCobranca(perfilId, enderecoId);
            endereco.setPadraoCobranca(true);
        }
        enderecoRepository.flush();
        log.info("Padrão atualizado em endereço {} (usuário {})", enderecoId, usuarioId);
        return toResponse(endereco);
    }

    private Endereco loadOwned(Long usuarioId, Long enderecoId) {
        var endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço " + enderecoId + " não encontrado"));
        var ownerUserId = endereco.getPerfilCliente().getUsuario().getId();
        if (!ownerUserId.equals(usuarioId)) {
            log.warn("Tentativa de acesso ao endereço {} pelo usuário {} (dono é {})",
                    enderecoId, usuarioId, ownerUserId);
            throw new ForbiddenException("Endereço pertence a outro usuário");
        }
        return endereco;
    }

    private PerfilCliente perfilOrCreate(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    var usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário " + usuarioId + " não encontrado"));
                    return perfilRepository.save(PerfilCliente.builder().usuario(usuario).build());
                });
    }

    private EnderecoResponse toResponse(Endereco e) {
        return new EnderecoResponse(
                e.getId(),
                e.getApelido(),
                e.getCep(),
                e.getLogradouro(),
                e.getNumero(),
                e.getComplemento(),
                e.getBairro(),
                e.getCidade(),
                e.getUf(),
                e.getPais(),
                e.getTipo(),
                e.isPadraoEntrega(),
                e.isPadraoCobranca(),
                e.isAtivo()
        );
    }
}
