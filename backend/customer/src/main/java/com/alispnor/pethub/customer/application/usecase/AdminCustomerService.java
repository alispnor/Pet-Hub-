package com.alispnor.pethub.customer.application.usecase;

import com.alispnor.pethub.common.dto.PageableResponse;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.application.dto.AdminCustomerDetail;
import com.alispnor.pethub.customer.application.dto.AdminCustomerSummary;
import com.alispnor.pethub.customer.infrastructure.persistence.EnderecoRepository;
import com.alispnor.pethub.customer.infrastructure.persistence.FormaPagamentoRepository;
import com.alispnor.pethub.customer.infrastructure.persistence.PerfilClienteRepository;
import com.alispnor.pethub.customer.infrastructure.persistence.PetRepository;
import com.alispnor.pethub.identity.domain.entity.TipoUsuario;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilClienteRepository perfilRepository;
    private final PetRepository petRepository;
    private final EnderecoRepository enderecoRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;

    @Transactional(readOnly = true)
    public PageableResponse<AdminCustomerSummary> listar(String q, Pageable pageable) {
        log.info("Admin: listando clientes (q={}, page={}, size={})", q, pageable.getPageNumber(), pageable.getPageSize());
        var hasQuery = q != null && !q.isBlank();
        var raw = hasQuery
                ? usuarioRepository.searchByTipo(TipoUsuario.CLIENTE, q.trim(), pageable)
                : usuarioRepository.findByTipo(TipoUsuario.CLIENTE, pageable);
        var page = raw.map(this::toSummary);
        log.info("Admin: {} clientes retornados (total={})", page.getNumberOfElements(), page.getTotalElements());
        return PageableResponse.of(page);
    }

    @Transactional(readOnly = true)
    public AdminCustomerDetail detalhar(Long usuarioId) {
        log.info("Admin: detalhando cliente {}", usuarioId);
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + usuarioId + " não encontrado"));
        if (usuario.getTipo() != TipoUsuario.CLIENTE) {
            throw new ResourceNotFoundException("Cliente " + usuarioId + " não encontrado");
        }
        var perfil = perfilRepository.findByUsuarioId(usuarioId).orElse(null);
        var perfilId = perfil == null ? null : perfil.getId();

        long totalPets = perfilId == null ? 0L : petRepository.findAllByPerfilClienteIdOrderByNomeAsc(perfilId).size();
        long totalEnderecos = perfilId == null ? 0L : enderecoRepository.findAllByPerfilClienteIdOrderByApelidoAsc(perfilId).size();
        long totalFormas = perfilId == null ? 0L : formaPagamentoRepository.countByPerfilClienteId(perfilId);

        var detail = new AdminCustomerDetail(
                usuario.getId(),
                usuario.getNome(),
                maskEmail(usuario.getEmail()),
                maskTelefone(usuario.getTelefone()),
                usuario.isAtivo(),
                usuario.getDataCadastro(),
                usuario.getUltimoLogin(),
                perfil == null ? null : perfil.getDataNascimento(),
                perfil == null ? null : perfil.getGenero(),
                perfil == null ? null : maskCpf(perfil.getCpfCriptografado()),
                perfil != null && perfil.isAceiteTermos(),
                perfil != null && perfil.isAceiteMarketing(),
                totalPets,
                totalEnderecos,
                totalFormas
        );
        log.info("Admin: cliente {} detalhado (pets={}, enderecos={}, formas={})",
                usuarioId, totalPets, totalEnderecos, totalFormas);
        return detail;
    }

    private AdminCustomerSummary toSummary(Usuario u) {
        var perfilId = perfilRepository.findByUsuarioId(u.getId()).map(p -> p.getId()).orElse(null);
        long totalPets = perfilId == null ? 0L : petRepository.findAllByPerfilClienteIdOrderByNomeAsc(perfilId).size();
        long totalEnderecos = perfilId == null ? 0L : enderecoRepository.findAllByPerfilClienteIdOrderByApelidoAsc(perfilId).size();
        return new AdminCustomerSummary(
                u.getId(),
                u.getNome(),
                maskEmail(u.getEmail()),
                u.isAtivo(),
                u.getDataCadastro(),
                totalPets,
                totalEnderecos
        );
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        var at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at);
        var head = email.charAt(0);
        var tail = email.charAt(at - 1);
        return head + "***" + tail + email.substring(at);
    }

    private String maskTelefone(String telefone) {
        if (telefone == null) return null;
        var digits = telefone.replaceAll("\\D", "");
        if (digits.length() < 4) return "****";
        return "******" + digits.substring(digits.length() - 4);
    }

    private String maskCpf(String cpfPlain) {
        if (cpfPlain == null || cpfPlain.length() != 11) return null;
        return "***." + cpfPlain.substring(3, 6) + "." + cpfPlain.substring(6, 9) + "-**";
    }
}
