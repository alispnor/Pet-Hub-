package com.alispnor.pethub.payment.infrastructure;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.payment.domain.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementação de desenvolvimento do {@link PaymentGateway}. Aprova ~90% dos
 * charges para exercitar o caminho feliz e rejeita ~10% para validar o tratamento
 * de erros pelo checkout. Em produção será substituído por
 * `MercadoPagoSandboxGateway` (Fase 3+) e pelo gateway real (Fase 11+).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "payment", name = "gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private final double approvalRate;

    public MockPaymentGateway(@Value("${payment.mock.approval-rate:0.9}") double approvalRate) {
        if (approvalRate < 0.0 || approvalRate > 1.0) {
            throw new IllegalArgumentException("payment.mock.approval-rate fora de [0,1]: " + approvalRate);
        }
        this.approvalRate = approvalRate;
    }

    // ============================= TOKENIZE =============================

    @Override
    public TokenizationResult tokenizeCard(CardData card) {
        log.info("Tokenizando cartão (mock) — mes={}, ano={}, len={}",
                card.validadeMes(), card.validadeAno(), digitsOnly(card.numero()).length());

        var numero = digitsOnly(card.numero());
        if (numero.length() < 13 || numero.length() > 19) {
            throw new BusinessRuleException("Número do cartão deve ter entre 13 e 19 dígitos");
        }
        if (!passesLuhn(numero)) {
            throw new BusinessRuleException("Número do cartão inválido (dígito verificador)");
        }
        var cvv = digitsOnly(card.cvv() == null ? "" : card.cvv());
        if (cvv.length() < 3 || cvv.length() > 4) {
            throw new BusinessRuleException("CVV deve ter 3 ou 4 dígitos");
        }
        if (card.validadeMes() < 1 || card.validadeMes() > 12) {
            throw new BusinessRuleException("Mês de validade fora do intervalo");
        }
        var hoje = LocalDate.now();
        var fim = LocalDate.of(card.validadeAno(), card.validadeMes(), 1).plusMonths(1).minusDays(1);
        if (fim.isBefore(hoje)) {
            throw new BusinessRuleException("Cartão expirado");
        }
        if (card.nomeImpresso() == null || card.nomeImpresso().isBlank()) {
            throw new BusinessRuleException("Nome impresso obrigatório");
        }

        var brand = detectarBrand(numero);
        var ultimos = numero.substring(numero.length() - 4);
        var token = "tok_mock_" + UUID.randomUUID();

        log.info("Cartão tokenizado: brand={}, ultimos={}, token={}", brand, ultimos, token);
        return new TokenizationResult(token, brand, ultimos);
    }

    // ============================= CHARGE =============================

    @Override
    public PaymentResult charge(ChargeRequest request) {
        log.info("Charge enter — pedido={}, method={}, valor={}, idem={}",
                request.referenciaPedido(), request.method(), request.valor(), request.idempotencyKey());

        if (request.valor() == null || request.valor().signum() <= 0) {
            throw new BusinessRuleException("Valor do charge precisa ser positivo");
        }
        if (request.method() == Method.CARTAO_CREDITO || request.method() == Method.CARTAO_DEBITO) {
            if (request.gatewayToken() == null || request.gatewayToken().isBlank()) {
                throw new BusinessRuleException("Charge em cartão exige gatewayToken");
            }
        }

        var transactionId = "txn_mock_" + UUID.randomUUID();
        var rng = ThreadLocalRandom.current().nextDouble();
        var approved = rng < approvalRate;

        String qrCode = null;
        String boletoUrl = null;
        if (request.method() == Method.PIX) {
            qrCode = generatePixMockQrCode(request.valor(), transactionId);
        } else if (request.method() == Method.BOLETO) {
            boletoUrl = "https://pethub.com/files/boletos/" + transactionId + ".pdf";
        }

        var status = approved ? Status.APPROVED : Status.REJECTED;
        var raw = "{\"transactionId\":\"" + transactionId + "\",\"status\":\"" + status
                + "\",\"rng\":" + rng + ",\"threshold\":" + approvalRate + "}";

        log.info("Charge exit — txn={}, status={} (rng={}, threshold={})",
                transactionId, status, rng, approvalRate);
        return new PaymentResult(transactionId, status, request.method(), request.valor(), qrCode, boletoUrl, raw);
    }

    @Override
    public RefundResult refund(String transactionId, BigDecimal valor) {
        log.info("Refund enter — txn={}, valor={}", transactionId, valor);
        if (transactionId == null || !transactionId.startsWith("txn_mock_")) {
            throw new BusinessRuleException("transactionId desconhecido: " + transactionId);
        }
        var refundId = "rfd_mock_" + UUID.randomUUID();
        log.info("Refund exit — refund={}, valor={}", refundId, valor);
        return new RefundResult(refundId, Status.REFUNDED, valor);
    }

    @Override
    public Status getStatus(String transactionId) {
        log.info("getStatus enter — txn={}", transactionId);
        if (transactionId == null || !transactionId.startsWith("txn_mock_")) {
            throw new BusinessRuleException("transactionId desconhecido: " + transactionId);
        }
        // Em produção iria consultar o gateway. Aqui assumimos que se chegou
        // até aqui o charge anterior já retornou um status definitivo.
        log.info("getStatus exit — txn={}, status=APPROVED (mock)", transactionId);
        return Status.APPROVED;
    }

    // ============================= helpers =============================

    private String generatePixMockQrCode(BigDecimal valor, String transactionId) {
        return "00020126360014BR.GOV.BCB.PIX0114pethub-mock-" + transactionId
                + "5204000053039865406" + valor.toPlainString() + "5802BR5908Pet Hub6009SAO PAULO62070503***6304MOCK";
    }

    private String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private boolean passesLuhn(String numero) {
        var sum = 0;
        var alternate = false;
        for (var i = numero.length() - 1; i >= 0; i--) {
            var n = numero.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private Brand detectarBrand(String numero) {
        if (numero.startsWith("4")) return Brand.VISA;
        if (numero.startsWith("34") || numero.startsWith("37")) return Brand.AMEX;
        var prefix2 = Integer.parseInt(numero.substring(0, 2));
        if (prefix2 >= 51 && prefix2 <= 55) return Brand.MASTER;
        var prefix4 = Integer.parseInt(numero.substring(0, 4));
        if (prefix4 >= 2221 && prefix4 <= 2720) return Brand.MASTER;
        if (numero.startsWith("6062") || numero.startsWith("5067") || numero.startsWith("4576")) return Brand.ELO;
        if (numero.startsWith("38") || numero.startsWith("60")) return Brand.HIPERCARD;
        return Brand.OUTRO;
    }
}
