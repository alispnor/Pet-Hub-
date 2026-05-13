package com.alispnor.pethub.customer.infrastructure.payment;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.customer.domain.entity.Bandeira;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementação de desenvolvimento do {@link PaymentGateway}: valida o formato
 * do cartão (Luhn + validade) e retorna um token fake `tok_mock_<uuid>`.
 * Na Fase 3 será substituído pela integração real (Mercado Pago sandbox ou Stripe).
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

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

        var bandeira = detectarBandeira(numero);
        var ultimos = numero.substring(numero.length() - 4);
        var token = "tok_mock_" + UUID.randomUUID();

        log.info("Cartão tokenizado: bandeira={}, ultimos={}, token={}", bandeira, ultimos, token);
        return new TokenizationResult(token, bandeira, ultimos);
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

    private Bandeira detectarBandeira(String numero) {
        if (numero.startsWith("4")) return Bandeira.VISA;
        if (numero.startsWith("34") || numero.startsWith("37")) return Bandeira.AMEX;
        var prefix2 = Integer.parseInt(numero.substring(0, 2));
        if (prefix2 >= 51 && prefix2 <= 55) return Bandeira.MASTER;
        var prefix4 = Integer.parseInt(numero.substring(0, 4));
        if (prefix4 >= 2221 && prefix4 <= 2720) return Bandeira.MASTER;
        if (numero.startsWith("6062") || numero.startsWith("5067") || numero.startsWith("4576")) return Bandeira.ELO;
        if (numero.startsWith("38") || numero.startsWith("60")) return Bandeira.HIPERCARD;
        return Bandeira.OUTRO;
    }
}
