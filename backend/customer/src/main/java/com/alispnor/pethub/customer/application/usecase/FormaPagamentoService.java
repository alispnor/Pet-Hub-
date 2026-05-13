package com.alispnor.pethub.customer.application.usecase;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ForbiddenException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.application.dto.CreateFormaPagamentoRequest;
import com.alispnor.pethub.customer.application.dto.FormaPagamentoResponse;
import com.alispnor.pethub.customer.application.dto.TokenizeCardRequest;
import com.alispnor.pethub.customer.application.dto.TokenizeCardResponse;
import com.alispnor.pethub.customer.domain.entity.FormaPagamento;
import com.alispnor.pethub.customer.domain.entity.PerfilCliente;
import com.alispnor.pethub.customer.infrastructure.payment.PaymentGateway;
import com.alispnor.pethub.customer.infrastructure.persistence.FormaPagamentoRepository;
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
public class FormaPagamentoService {

    private final FormaPagamentoRepository formaPagamentoRepository;
    private final PerfilClienteRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentGateway paymentGateway;

    public TokenizeCardResponse tokenizar(TokenizeCardRequest request) {
        log.info("Tokenizando cartão via gateway");
        var result = paymentGateway.tokenizeCard(new PaymentGateway.CardData(
                request.numero(),
                request.cvv(),
                request.nomeImpresso(),
                request.validadeMes(),
                request.validadeAno()
        ));
        log.info("Cartão tokenizado: bandeira={}, ultimos={}", result.bandeira(), result.ultimosQuatroDigitos());
        return new TokenizeCardResponse(result.token(), result.bandeira(), result.ultimosQuatroDigitos());
    }

    @Transactional(readOnly = true)
    public List<FormaPagamentoResponse> listar(Long usuarioId) {
        log.info("Listando formas de pagamento do usuário {}", usuarioId);
        var perfil = perfilRepository.findByUsuarioId(usuarioId).orElse(null);
        if (perfil == null) {
            return List.of();
        }
        var formas = formaPagamentoRepository.findAllByPerfilClienteIdOrderByCriadoEmDesc(perfil.getId()).stream()
                .map(this::toResponse)
                .toList();
        log.info("Encontradas {} formas de pagamento para usuário {}", formas.size(), usuarioId);
        return formas;
    }

    @Transactional
    public FormaPagamentoResponse criar(Long usuarioId, CreateFormaPagamentoRequest request) {
        log.info("Criando forma de pagamento tipo={} para usuário {}", request.tipo(), usuarioId);
        validarPayload(request);

        var perfil = perfilOrCreate(usuarioId);
        var isFirst = formaPagamentoRepository.countByPerfilClienteId(perfil.getId()) == 0;
        var marcarPadrao = Boolean.TRUE.equals(request.padrao()) || isFirst;

        var entity = FormaPagamento.builder()
                .perfilCliente(perfil)
                .tipo(request.tipo())
                .apelido(request.apelido())
                .gatewayToken(request.tipo().exigeCartao() ? request.gatewayToken() : null)
                .bandeira(request.tipo().exigeCartao() ? request.bandeira() : null)
                .ultimosQuatroDigitos(request.tipo().exigeCartao() ? request.ultimosQuatroDigitos() : null)
                .nomeImpresso(request.tipo().exigeCartao() ? request.nomeImpresso() : null)
                .validadeMes(request.tipo().exigeCartao() ? request.validadeMes() : null)
                .validadeAno(request.tipo().exigeCartao() ? request.validadeAno() : null)
                .padrao(false)
                .ativo(true)
                .build();
        var saved = formaPagamentoRepository.save(entity);

        if (marcarPadrao) {
            formaPagamentoRepository.clearOtherDefault(perfil.getId(), saved.getId());
            saved.setPadrao(true);
            formaPagamentoRepository.flush();
        }

        log.info("Forma de pagamento {} criada para usuário {} (padrao={})", saved.getId(), usuarioId, marcarPadrao);
        return toResponse(saved);
    }

    @Transactional
    public void remover(Long usuarioId, Long formaId) {
        log.info("Removendo forma de pagamento {} do usuário {}", formaId, usuarioId);
        var forma = loadOwned(usuarioId, formaId);
        formaPagamentoRepository.delete(forma);
        log.info("Forma de pagamento {} removida (usuário {})", formaId, usuarioId);
    }

    @Transactional
    public FormaPagamentoResponse definirPadrao(Long usuarioId, Long formaId) {
        log.info("Definindo forma de pagamento {} como padrão (usuário {})", formaId, usuarioId);
        var forma = loadOwned(usuarioId, formaId);
        var perfilId = forma.getPerfilCliente().getId();
        formaPagamentoRepository.clearOtherDefault(perfilId, formaId);
        forma.setPadrao(true);
        formaPagamentoRepository.flush();
        log.info("Forma de pagamento {} marcada como padrão (usuário {})", formaId, usuarioId);
        return toResponse(forma);
    }

    private void validarPayload(CreateFormaPagamentoRequest request) {
        if (request.tipo().exigeCartao()) {
            if (request.gatewayToken() == null || request.gatewayToken().isBlank()) {
                throw new BusinessRuleException("Cartão exige gatewayToken (chame /tokenize antes)");
            }
            if (request.bandeira() == null
                    || request.ultimosQuatroDigitos() == null
                    || request.validadeMes() == null
                    || request.validadeAno() == null) {
                throw new BusinessRuleException("Cartão exige bandeira, últimos 4 dígitos e validade");
            }
        }
    }

    private FormaPagamento loadOwned(Long usuarioId, Long formaId) {
        var forma = formaPagamentoRepository.findById(formaId)
                .orElseThrow(() -> new ResourceNotFoundException("Forma de pagamento " + formaId + " não encontrada"));
        var ownerUserId = forma.getPerfilCliente().getUsuario().getId();
        if (!ownerUserId.equals(usuarioId)) {
            log.warn("Tentativa de acesso à forma {} pelo usuário {} (dono é {})",
                    formaId, usuarioId, ownerUserId);
            throw new ForbiddenException("Forma de pagamento pertence a outro usuário");
        }
        return forma;
    }

    private PerfilCliente perfilOrCreate(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    var usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário " + usuarioId + " não encontrado"));
                    return perfilRepository.save(PerfilCliente.builder().usuario(usuario).build());
                });
    }

    private FormaPagamentoResponse toResponse(FormaPagamento f) {
        return new FormaPagamentoResponse(
                f.getId(),
                f.getTipo(),
                f.getApelido(),
                f.getBandeira(),
                f.getUltimosQuatroDigitos(),
                f.getNomeImpresso(),
                f.getValidadeMes(),
                f.getValidadeAno(),
                f.isPadrao(),
                f.isAtivo()
        );
    }
}
